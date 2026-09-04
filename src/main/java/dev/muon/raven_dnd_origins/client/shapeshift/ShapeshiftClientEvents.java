package dev.muon.raven_dnd_origins.client.shapeshift;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = RavenDndOrigins.MODID)
public class ShapeshiftClientEvents {

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ShapeshiftClientState.clear();
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        if (!player.getEquipmentSlotForItem(event.getItemStack()).isArmor()) return;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config != null && config.suppressArmorModifiers()) {
            event.getToolTip().add(
                    Component.translatable("tooltip.raven_dnd_origins.armor_suppressed")
                            .withStyle(ChatFormatting.RED)
            );
        }
    }
}
