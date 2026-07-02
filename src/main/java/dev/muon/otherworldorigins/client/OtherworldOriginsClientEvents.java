package dev.muon.otherworldorigins.client;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.client.screen.OtherworldOriginScreen;
import dev.muon.otherworldorigins.network.RequestContainerSyncMessage;
import dev.muon.otherworldorigins.network.RequestFullSyncMessage;
import dev.muon.otherworldorigins.selection.ClientSelectionState;
import dev.muon.otherworldorigins.selection.SelectionSession;
import dev.muon.otherworldorigins.selection.SessionKind;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.client.OriginsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OtherworldOriginsClientEvents {

    /**
     * On login, ask the server for a full re-sync (dynamic registries + per-player containers).
     * On respawn / dimension change (both fire {@link ClientPlayerNetworkEvent.Clone}), ask for
     * a container-only re-sync — the registries don't change between dimensions.
     *
     * <p>Both packets sidestep the {@code TRACKING_ENTITY_AND_SELF} race where server-initiated
     * syncs can arrive while the client's new {@code LocalPlayer} isn't yet registered in the new
     * {@code ClientLevel}; see {@link RequestFullSyncMessage} / {@link RequestContainerSyncMessage}.</p>
     */
    @SubscribeEvent
    public static void onClientPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        OtherworldOrigins.CHANNEL.sendToServer(new RequestFullSyncMessage());
    }

    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        OtherworldOrigins.CHANNEL.sendToServer(new RequestContainerSyncMessage());
    }

    /**
     * Opens the selection screen when the server raises {@code AWAITING_DISPLAY}. Runs at HIGH
     * priority to pre-empt Origins' own handler. Layers and mode come from the synced
     * {@link SelectionSession}; with no session (e.g. an Origins-native orb) it falls back to a
     * full creation screen over every layer.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft instance = Minecraft.getInstance();
        if (!OriginsClient.AWAITING_DISPLAY.get() || instance.screen != null || instance.player == null) {
            return;
        }

        Registry<OriginLayer> registry = OriginsAPI.getLayersRegistry(null);
        SelectionSession session = ClientSelectionState.get();

        List<Holder<OriginLayer>> layers = new ArrayList<>();
        SessionKind kind;
        if (session != null && !session.layers().isEmpty()) {
            kind = session.kind();
            for (ResourceLocation id : session.layers()) {
                OriginLayer layer = registry.get(id);
                if (layer != null) {
                    registry.getResourceKey(layer).flatMap(registry::getHolder).ifPresent(layers::add);
                }
            }
        } else {
            kind = SessionKind.INITIAL_CREATION;
            OriginsAPI.getActiveLayers().stream()
                    .sorted(Comparator.comparing(Holder::value))
                    .forEach(layers::add);
        }

        if (!layers.isEmpty()) {
            // Reselection orbs are used mid-game with a world behind the screen; never dirt there.
            boolean showDirtBackground = kind != SessionKind.RESELECTION && OriginsClient.SHOW_DIRT_BACKGROUND;
            instance.setScreen(new OtherworldOriginScreen(layers, 0, showDirtBackground, kind));
            OriginsClient.AWAITING_DISPLAY.set(false);
        }
    }
}
