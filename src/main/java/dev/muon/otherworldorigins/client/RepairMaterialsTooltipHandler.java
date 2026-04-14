package dev.muon.otherworldorigins.client;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.power.ShowRepairTooltipPower;
import dev.muon.otherworldorigins.util.RepairMaterialDescription;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Shift-expandable repair info (same idea as AttributesLib merged tooltips: {@link Screen#hasShiftDown()}).
 */
@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RepairMaterialsTooltipHandler {

    private RepairMaterialsTooltipHandler() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        if (!ShowRepairTooltipPower.has(player)) {
            return;
        }
        if (!RepairMaterialDescription.shouldShow(event.getItemStack())) {
            return;
        }
        List<Component> tip = event.getToolTip();
        if (Screen.hasShiftDown()) {
            tip.addAll(RepairMaterialDescription.expandedLines(event.getItemStack()));
        } else {
            tip.add(RepairMaterialDescription.collapsedLine());
        }
    }
}
