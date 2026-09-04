package dev.muon.raven_dnd_origins.client;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.client.screen.RavenDndOriginScreen;
import dev.muon.raven_dnd_origins.network.RequestContainerSyncMessage;
import dev.muon.raven_dnd_origins.network.RequestFullSyncMessage;
import dev.muon.raven_dnd_origins.selection.ClientSelectionState;
import dev.muon.raven_dnd_origins.selection.SelectionSession;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import dev.overgrown.origins.client.screen.ChooseOriginScreen;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT, modid = RavenDndOrigins.MODID)
public class RavenDndOriginsClientEvents {

    /**
     * The session instance a screen was last opened for. Compared by identity, so a re-issued prompt
     * (a fresh packet, hence a fresh record) re-opens the screen while the current one does not
     * re-open it every tick, nor between {@code finishSelection} and the server clearing the session.
     */
    @Nullable
    private static SelectionSession displayedSession;

    /**
     * On login, ask the server for a full re-sync (dynamic registries + per-player containers).
     * On respawn / dimension change (both fire {@link ClientPlayerNetworkEvent.Clone}), ask for
     * a container-only re-sync; the registries don't change between dimensions.
     *
     * <p>Both packets sidestep the {@code TRACKING_ENTITY_AND_SELF} race where server-initiated
     * syncs can arrive while the client's new {@code LocalPlayer} isn't yet registered in the new
     * {@code ClientLevel}; see {@link RequestFullSyncMessage} / {@link RequestContainerSyncMessage}.</p>
     */
    @SubscribeEvent
    public static void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // Fired from handleLogin, before any play-phase payload of this connection is handled, so a
        // session left over from a previous world can never outlive the join it belonged to.
        ClientSelectionState.clear();
        displayedSession = null;
        PacketDistributor.sendToServer(new RequestFullSyncMessage());
    }

    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        PacketDistributor.sendToServer(new RequestContainerSyncMessage());
    }

    /**
     * Opens the selection screen for the pending {@link SelectionSession} once nothing else is on
     * screen. The server only closes the current screen and syncs the session, so this is the sole
     * trigger for every mod-driven prompt (orb, level-up feat, power prompt).
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SelectionSession session = ClientSelectionState.get();
        if (session == null || session.isEmpty()) {
            displayedSession = null;
            return;
        }

        Minecraft instance = Minecraft.getInstance();
        if (session == displayedSession || instance.player == null || instance.screen != null) {
            return;
        }

        List<OriginLayer> layers = resolve(session.layers());
        if (!layers.isEmpty()) {
            displayedSession = session;
            instance.setScreen(new RavenDndOriginScreen(layers, 0, session.kind()));
        }
    }

    /**
     * Origins pushes its own single-layer {@link ChooseOriginScreen} on login and again after every
     * confirmed pick. Ours walks the session's layers itself, so those pushes are dropped rather than
     * allowed to rebuild it from layer index 0. With no session (e.g. an Origins-native orb) the push
     * is swapped for a full creation screen over every enabled layer.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof ChooseOriginScreen)) {
            return;
        }

        SelectionSession session = ClientSelectionState.get();
        if (event.getCurrentScreen() instanceof RavenDndOriginScreen
                || (session != null && !session.isEmpty())) {
            event.setCanceled(true);
            return;
        }

        List<OriginLayer> layers = new ArrayList<>(OriginLayers.enabledOrdered());
        if (!layers.isEmpty()) {
            event.setNewScreen(new RavenDndOriginScreen(layers, 0, SessionKind.INITIAL_CREATION));
        }
    }

    private static List<OriginLayer> resolve(List<ResourceLocation> layerIds) {
        List<OriginLayer> layers = new ArrayList<>();
        for (ResourceLocation id : layerIds) {
            OriginLayer layer = OriginLayers.get(id);
            if (layer != null) {
                layers.add(layer);
            }
        }
        return layers;
    }
}
