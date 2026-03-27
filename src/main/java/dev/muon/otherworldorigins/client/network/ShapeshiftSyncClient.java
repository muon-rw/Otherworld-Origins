package dev.muon.otherworldorigins.client.network;

import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftClientState;
import dev.muon.otherworldorigins.network.ShapeshiftSyncMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ShapeshiftSyncClient {

    private ShapeshiftSyncClient() {}

    public static void handlePacket(ShapeshiftSyncMessage message) {
        ShapeshiftClientState.handleSync(
                message.playerId(), message.entityType(),
                message.hideHands(), message.allowTools(),
                message.collisionWidth(), message.collisionHeight());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(message.playerId());
            if (entity instanceof Player player) {
                player.refreshDimensions();
            }
        }
    }
}
