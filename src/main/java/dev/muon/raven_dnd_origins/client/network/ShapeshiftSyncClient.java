package dev.muon.raven_dnd_origins.client.network;

import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftClientState;
import dev.muon.raven_dnd_origins.network.ShapeshiftSyncMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ShapeshiftSyncClient {

    private ShapeshiftSyncClient() {}

    public static void handlePacket(ShapeshiftSyncMessage message) {
        ShapeshiftClientState.handleSync(
                message.playerId(), message.entityType(),
                message.hideHands(), message.allowTools(),
                message.collisionWidth(), message.collisionHeight(), message.flightSpeed());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(message.playerId());
            if (entity instanceof Player player) {
                player.refreshDimensions();
            }
        }
    }
}
