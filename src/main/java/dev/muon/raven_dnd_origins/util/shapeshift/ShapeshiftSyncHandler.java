package dev.muon.raven_dnd_origins.util.shapeshift;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.network.ShapeshiftSyncMessage;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class ShapeshiftSyncHandler {

    private static final Map<UUID, ShapeshiftPower.Configuration> LAST_KNOWN_STATE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        tickAquaticWildshapeSwimming(player);

        // Sampled every tick so a form cannot start and end unseen; the network sync only fires on change
        ShapeshiftPower.Configuration current = ShapeshiftPower.getActiveShapeshiftConfig(player);
        ShapeshiftPower.Configuration previous = LAST_KNOWN_STATE.get(player.getUUID());

        boolean changed = (current == null) != (previous == null)
                || (current != null && !current.syncFieldsEqual(previous));
        if (changed) {
            updateState(player.getUUID(), current);
            broadcastToTracking(player, current);
            if (current != null && current.suppressArmorModifiers()) {
                ShapeshiftEquipmentHandler.suppressArmorModifiers(player);
            } else if (previous != null && previous.suppressArmorModifiers()) {
                ShapeshiftEquipmentHandler.restoreArmorModifiers(player);
            }
            player.refreshDimensions();
            // refreshDimensions() can clear the swim flag; re-apply immediately
            tickAquaticWildshapeSwimming(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        resync(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        resync(player);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof ServerPlayer tracked)) return;
        if (!(event.getEntity() instanceof ServerPlayer tracker)) return;
        ShapeshiftPower.Configuration current = ShapeshiftPower.getActiveShapeshiftConfig(tracked);
        if (current != null) {
            PacketDistributor.sendToPlayer(tracker, createMessage(tracked.getId(), current));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_KNOWN_STATE.remove(event.getEntity().getUUID());
    }

    private static void resync(ServerPlayer player) {
        ShapeshiftPower.Configuration current = ShapeshiftPower.getActiveShapeshiftConfig(player);
        updateState(player.getUUID(), current);
        broadcastToTracking(player, current);
        player.refreshDimensions();
        tickAquaticWildshapeSwimming(player);
    }

    private static void broadcastToTracking(ServerPlayer player, @Nullable ShapeshiftPower.Configuration config) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, createMessage(player.getId(), config));
    }

    private static ShapeshiftSyncMessage createMessage(int playerId, @Nullable ShapeshiftPower.Configuration config) {
        if (config == null) {
            return new ShapeshiftSyncMessage(playerId, null, false, true, 0.0F, 0.0F, 1.0F);
        }
        ShapeshiftCollisionShape shape = config.effectiveCollisionShape();
        return new ShapeshiftSyncMessage(
                playerId,
                config.entityType(),
                config.hideHands(),
                config.allowTools(),
                shape.width(),
                shape.height(),
                config.flightSpeed());
    }

    private static void updateState(UUID uuid, @Nullable ShapeshiftPower.Configuration config) {
        if (config != null) {
            LAST_KNOWN_STATE.put(uuid, config);
        } else {
            LAST_KNOWN_STATE.remove(uuid);
        }
    }

    /**
     * Vanilla only sets {@linkplain net.minecraft.world.entity.Entity#isSwimming() swimming} when
     * sprinting underwater; aquatic wildshapes should use the swim pose whenever
     * {@linkplain net.minecraft.world.entity.Entity#isInWater()} so client animations and cross-mod
     * checks behave correctly.
     */
    private static void tickAquaticWildshapeSwimming(ServerPlayer player) {
        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null || !config.autoSwimInWater()) return;
        player.setSwimming(player.isInWater());
    }
}
