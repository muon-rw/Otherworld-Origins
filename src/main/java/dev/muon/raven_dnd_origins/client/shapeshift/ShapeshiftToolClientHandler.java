package dev.muon.raven_dnd_origins.client.shapeshift;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftToolHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * The server only learns about a blocked weapon swing when it connects with a target, so a swing
 * at air never produced the "cannot wield" message. Refusing the attack key on the client covers
 * every swing, Better Combat's included, since it never starts its own attack without the key.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = RavenDndOrigins.MODID)
public class ShapeshiftToolClientHandler {
    private static final int MESSAGE_INTERVAL_TICKS = 10;
    private static int lastMessageTick = -MESSAGE_INTERVAL_TICKS;

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null || !ShapeshiftToolHandler.isBlockedHeldItem(player)) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (player.tickCount - lastMessageTick >= MESSAGE_INTERVAL_TICKS) {
            lastMessageTick = player.tickCount;
            ShapeshiftToolHandler.notifyBlocked(player);
        }
    }
}
