package dev.muon.otherworldorigins.client.shapeshift;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShapeshiftClientEvents {

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ShapeshiftClientState.clear();
    }

    /**
     * Suppress all nametag/healthbar rendering on the fake shapeshift entity body.
     * MobHealthBar, LevelDisplayRenderer, and vanilla nametags all fire through
     * RenderNameTagEvent. When we're rendering the morph body, deny everything --
     * the player's own nametag is re-rendered separately via the player renderer.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (ShapeshiftRenderHelper.isRenderingShapeshiftBody()) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        if (!(event.getItemStack().getItem() instanceof ArmorItem)) return;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config != null && config.preventEquipment()) {
            event.getToolTip().add(
                    Component.translatable("tooltip.otherworldorigins.equipment_prevented")
                            .withStyle(ChatFormatting.RED)
            );
        }
    }
}
