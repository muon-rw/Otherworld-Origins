package dev.muon.raven_dnd_origins.client;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.power.ShowRepairTooltipPower;
import dev.muon.raven_dnd_origins.util.RepairMaterialDescription;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Shift-expandable repair info (same idea as AttributesLib merged tooltips: {@link Screen#hasShiftDown()}).
 */
@EventBusSubscriber(modid = RavenDndOrigins.MODID, value = Dist.CLIENT)
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
