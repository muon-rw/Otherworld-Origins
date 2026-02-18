package dev.muon.otherworldorigins.client;

import com.mojang.datafixers.util.Either;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.item.GoodberryItem;
import dev.muon.otherworldorigins.item.HeartsTooltipComponent;
import dev.muon.otherworldorigins.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GoodberryTooltipHandler {

    private static final int HEARTS_COUNT = 4;

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ModItems.GOODBERRY.get())) return;
        var level = Minecraft.getInstance().level;
        boolean decayed = level != null && GoodberryItem.isDecayed(event.getItemStack(), level);

        if (!decayed) {
            List<Component> tooltips = event.getToolTip();
            if (!tooltips.isEmpty()) {
                Component name = tooltips.get(0);
                tooltips.set(0, name.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        if (!event.getItemStack().is(ModItems.GOODBERRY.get())) return;
        var level = Minecraft.getInstance().level;
        if (level != null && GoodberryItem.isDecayed(event.getItemStack(), level)) return;

        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        elements.add(1, Either.right(new HeartsTooltipComponent(HEARTS_COUNT)));
    }
}
