package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.ShapeshiftSyncMessage;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShapeshiftSyncHandler {

    private static final Map<UUID, ShapeshiftPower.Configuration> LAST_KNOWN_STATE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;

        tickAquaticWildshapeSwimming(player);

        if (player.tickCount % 5 != 0) return;

        ShapeshiftPower.Configuration current = ShapeshiftPower.getActiveShapeshiftConfig(player);
        ShapeshiftPower.Configuration previous = LAST_KNOWN_STATE.get(player.getUUID());

        boolean changed = (current == null) != (previous == null)
                || (current != null && !current.syncFieldsEqual(previous));
        if (changed) {
            updateState(player.getUUID(), current);
            broadcastToTracking(player, current);
            player.refreshDimensions();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ShapeshiftPower.Configuration current = ShapeshiftPower.getActiveShapeshiftConfig(player);
        updateState(player.getUUID(), current);
        broadcastToTracking(player, current);
        player.refreshDimensions();
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ShapeshiftPower.Configuration current = ShapeshiftPower.getActiveShapeshiftConfig(player);
        updateState(player.getUUID(), current);
        broadcastToTracking(player, current);
        player.refreshDimensions();
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof ServerPlayer tracked)) return;
        if (!(event.getEntity() instanceof ServerPlayer tracker)) return;
        ShapeshiftPower.Configuration current = ShapeshiftPower.getActiveShapeshiftConfig(tracked);
        if (current != null) {
            OtherworldOrigins.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> tracker),
                    createMessage(tracked.getId(), current)
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_KNOWN_STATE.remove(event.getEntity().getUUID());
    }

    private static void broadcastToTracking(ServerPlayer player, @Nullable ShapeshiftPower.Configuration config) {
        ShapeshiftSyncMessage message = createMessage(player.getId(), config);
        OtherworldOrigins.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                message
        );
    }

    private static ShapeshiftSyncMessage createMessage(int playerId, @Nullable ShapeshiftPower.Configuration config) {
        if (config == null) {
            return new ShapeshiftSyncMessage(playerId, null, false, true, 0.0F, 0.0F);
        }
        var shape = config.effectiveCollisionShape();
        return new ShapeshiftSyncMessage(
                playerId,
                config.entityType(),
                config.hideHands(),
                config.allowTools(),
                shape.width(),
                shape.height());
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
