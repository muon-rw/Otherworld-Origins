package dev.muon.raven_dnd_origins.selection;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.network.CloseCurrentScreenMessage;
import dev.muon.raven_dnd_origins.network.OpenFinalConfirmScreenMessage;
import dev.muon.raven_dnd_origins.network.SyncSelectionSessionMessage;
import dev.muon.raven_dnd_origins.util.OriginStateDumper;
import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.SwapManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side authority for per-player {@link SelectionSession}s: persistence, the single
 * reconciliation step (run at login and at screen-finish), and issuing prompts to the client.
 *
 * <p>Stored in the player's {@code PlayerPersisted} NBT subtag, which NeoForge copies across death,
 * so the session survives disconnect, restart, and death with no attachment or clone handler.
 */
public final class SelectionSessions {

    private SelectionSessions() {}

    private static final String NBT_KEY = "raven_dnd_origins:selection_session";

    /**
     * Per-player count of consecutive re-prompts for an unresolved session, reset once the session
     * completes. Trips the loop guard in {@link #reconcile} if the screen and {@link #emptyValidLayers}
     * ever disagree on what is selectable; the player would otherwise be stuck on an unescapable
     * screen forever. Transient (server-uptime scoped); cleared on {@link #clear}.
     */
    private static final Map<UUID, Integer> reissueCount = new ConcurrentHashMap<>();
    private static final int MAX_REISSUES = 20;

    // --- persistence -------------------------------------------------------

    public static Optional<SelectionSession> get(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(NBT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return Optional.of(SelectionSession.fromNbt(persisted.getCompound(NBT_KEY)));
    }

    private static void store(ServerPlayer player, SelectionSession session) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(NBT_KEY, session.toNbt());
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static void clear(ServerPlayer player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.remove(NBT_KEY);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        reissueCount.remove(player.getUUID());
    }

    // --- session lifecycle -------------------------------------------------

    /**
     * Opens a fresh session and prompts the player, or, if a session is already open, merges
     * {@code layers}/{@code kind} into its record only. The live screen is left untouched; the
     * merged layers surface at the next finish-reconcile. (A session can only be open while the
     * player is on the non-escapable screen, so "session present" means "screen open".)
     */
    public static void beginOrMerge(ServerPlayer player, List<ResourceLocation> layers, SessionKind kind) {
        Optional<SelectionSession> existing = get(player);
        if (existing.isPresent()) {
            List<ResourceLocation> union = new ArrayList<>(existing.get().layers());
            for (ResourceLocation id : layers) {
                if (!union.contains(id)) {
                    union.add(id);
                }
            }
            SessionKind merged = SessionKind.dominant(existing.get().kind(), kind);
            store(player, new SelectionSession(orderByLayerOrder(union), merged));
            return;
        }
        store(player, new SelectionSession(orderByLayerOrder(layers), kind));
        reconcile(player);
    }

    /** Wipes every enabled layer and re-runs full character creation: orb, Start Over, gui commands. */
    public static boolean beginFullCreation(ServerPlayer player) {
        return beginCleared(player, activeLayerIds(), SessionKind.INITIAL_CREATION);
    }

    /**
     * Clears the given layers' origins, then opens (or merges) a session for them. For prompts
     * that re-pick already-chosen layers: reselection orbs and power-driven prompts like wildshape.
     *
     * @return true if at least one layer existed and was cleared
     */
    public static boolean beginCleared(ServerPlayer player, List<ResourceLocation> layers, SessionKind kind) {
        List<ResourceLocation> cleared = clearLayers(player, layers);
        if (cleared.isEmpty()) {
            return false;
        }
        beginOrMerge(player, cleared, kind);
        return true;
    }

    /**
     * Batched {@link OriginManager#removeOrigin}: every layer is cleared before the single
     * reconciliation pass. {@code removeOrigin} reconciles inside each call, which would let a layer
     * cleared early in the list be auto-chosen again while the later ones still hold stale origins,
     * dropping it out of the prompt the player is about to get.
     *
     * @return the layers that exist and were cleared, in the given order
     */
    public static List<ResourceLocation> clearLayers(ServerPlayer player, Collection<ResourceLocation> layers) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) {
            return List.of();
        }
        List<ResourceLocation> cleared = new ArrayList<>();
        for (ResourceLocation id : layers) {
            OriginLayer layer = OriginLayers.get(id);
            if (layer == null) {
                RavenDndOrigins.LOGGER.error("selection_session: layer '{}' does not exist", id);
                continue;
            }
            clearWithoutReconcile(player, state, layer);
            cleared.add(id);
        }
        if (!cleared.isEmpty()) {
            OriginManager.reconcileLayers(player);
        }
        return cleared;
    }

    private static void clearWithoutReconcile(ServerPlayer player, PlayerOriginsImpl state, OriginLayer layer) {
        ResourceLocation id = layer.id();
        if (layer.swappable()) {
            SwapManager.clearPool(player, id);
            return;
        }
        SwapManager.revokeSwapGrants(player, id);
        if (!state.hasOrigin(id)) {
            return;
        }
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container != null) {
            container.removeAllFromSource(layerPowerSource(id));
        }
        state.clearOrigin(id);
    }

    private static ResourceLocation layerPowerSource(ResourceLocation layerId) {
        return ResourceLocation.fromNamespaceAndPath(layerId.getNamespace(), "layer/" + layerId.getPath());
    }

    /** On level-up: prompt for any level-gated layer that is now unlocked but still unchosen. */
    public static void promptLevelGated(ServerPlayer player) {
        List<ResourceLocation> available = emptyValidLayers(player, SelectionLayers.LEVEL_GATED);
        if (available.isEmpty()) {
            return;
        }
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        beginOrMerge(player, available, SessionKind.LEVEL_UP);
    }

    /**
     * The single reconciliation step, run at login and at screen-finish: complete the session if
     * every layer is resolved, otherwise (re-)issue the prompt.
     *
     * <p>With no session stored, the empty-layer count is used to estimate intent: a single empty
     * layer is almost always one orphaned by a gameplay incidental, so it gets a scoped re-prompt;
     * two or more is the shape of a fresh character (the initial state exposes race + class), so it
     * falls back to a full {@code INITIAL_CREATION} prompt. (The rare case of two layers orphaned
     * at once thus also triggers full creation, acceptable since that state is genuinely
     * indistinguishable from a fresh login with wiped player data.)
     *
     * <p>If a stored session is re-issued {@link #MAX_REISSUES} times without resolving, the screen
     * and {@link #emptyValidLayers} disagreeing on selectability, the player is disconnected with a
     * state dump rather than left trapped on an unescapable screen.
     */
    public static void reconcile(ServerPlayer player) {
        Optional<SelectionSession> stored = get(player);
        if (stored.isEmpty()) {
            List<ResourceLocation> active = activeLayerIds();
            List<ResourceLocation> empties = emptyValidLayers(player, active);
            if (empties.size() == 1) {
                SelectionSession created = new SelectionSession(empties, SessionKind.LEVEL_UP);
                store(player, created);
                issuePrompt(player, created);
            } else if (empties.size() >= 2) {
                SelectionSession created = new SelectionSession(active, SessionKind.INITIAL_CREATION);
                store(player, created);
                issuePrompt(player, created);
            }
            return;
        }
        SelectionSession session = stored.get();
        if (emptyValidLayers(player, session.layers()).isEmpty()) {
            clear(player);
            onComplete(player, session);
            return;
        }
        int attempts = reissueCount.merge(player.getUUID(), 1, Integer::sum);
        if (attempts > MAX_REISSUES) {
            RavenDndOrigins.LOGGER.warn(
                    "[SelectionSession] {} re-prompted {} times without resolving layers {}; disconnecting",
                    player.getName().getString(), attempts, session.layers(),
                    new Throwable("selection-session re-issue loop tripwire; call path"));
            OriginStateDumper.dump(player, "SERVER", player.getServer(),
                    "selection session re-issue loop (" + attempts + " attempts, layers " + session.layers() + ")");
            clear(player);
            player.connection.disconnect(Component.translatable("raven_dnd_origins.disconnect.validation_failed"));
            return;
        }
        issuePrompt(player, session);
    }

    private static void issuePrompt(ServerPlayer player, SelectionSession session) {
        PacketDistributor.sendToPlayer(player, new CloseCurrentScreenMessage());
        PlayerOriginsAttachment.getOrCreate(player).setSelectingOrigin(true);
        OriginsServerNetwork.sendPlayerOriginsTo(player, player);
        PacketDistributor.sendToPlayer(player, new SyncSelectionSessionMessage(session));
    }

    private static void onComplete(ServerPlayer player, SelectionSession session) {
        PlayerOriginsAttachment.getOrCreate(player).setSelectingOrigin(false);
        if (player.getServer() != null) {
            OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
        }
        PacketDistributor.sendToPlayer(player, new SyncSelectionSessionMessage(null));
        if (session.kind() == SessionKind.INITIAL_CREATION || session.kind() == SessionKind.RESELECTION) {
            PacketDistributor.sendToPlayer(player,
                    new OpenFinalConfirmScreenMessage(session.kind(), session.layers()));
        }
    }

    // --- layer queries -----------------------------------------------------

    /** Of {@code candidates}, the layers currently empty and with valid options for the player. */
    private static List<ResourceLocation> emptyValidLayers(ServerPlayer player, Collection<ResourceLocation> candidates) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) {
            return List.of();
        }
        List<ResourceLocation> result = new ArrayList<>();
        for (ResourceLocation id : candidates) {
            OriginLayer layer = OriginLayers.get(id);
            if (layer == null || state.hasOrigin(id)) {
                continue;
            }
            if (OriginManager.hasChoosableOrigins(player, layer)) {
                result.add(id);
            }
        }
        return result;
    }

    private static List<ResourceLocation> activeLayerIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            ids.add(layer.id());
        }
        return ids;
    }

    /** Datapack layer order, so the screen evaluates parent layers before the layers gated on them. */
    private static List<ResourceLocation> orderByLayerOrder(Collection<ResourceLocation> ids) {
        List<ResourceLocation> ordered = new ArrayList<>();
        for (ResourceLocation id : activeLayerIds()) {
            if (ids.contains(id)) {
                ordered.add(id);
            }
        }
        return ordered;
    }
}
