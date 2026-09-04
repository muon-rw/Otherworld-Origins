package dev.muon.raven_dnd_origins.client;

import com.mojang.datafixers.util.Either;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.item.GoodberryItem;
import dev.muon.raven_dnd_origins.item.HeartsTooltipComponent;
import dev.muon.raven_dnd_origins.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * The decay line lives here rather than in {@code Item#appendHoverText} because 1.21 dropped the
 * {@code Level} parameter and the remaining time needs the world clock.
 */
@EventBusSubscriber(modid = RavenDndOrigins.MODID, value = Dist.CLIENT)
public class GoodberryTooltipHandler {

    private static final int HEARTS_COUNT = 4;

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.GOODBERRY.get())) return;
        ClientLevel level = Minecraft.getInstance().level;
        boolean decayed = level != null ? GoodberryItem.isDecayed(stack, level) : GoodberryItem.isDecayedCached(stack);

        List<Component> tooltips = event.getToolTip();
        if (!decayed && !tooltips.isEmpty()) {
            tooltips.set(0, tooltips.get(0).copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        Style darkGrayItalic = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true);
        if (decayed) {
            tooltips.add(Component.translatable("item.raven_dnd_origins.goodberry.tooltip.ordinary")
                    .withStyle(darkGrayItalic));
        } else if (level != null) {
            String remaining = formatTicksToTime(GoodberryItem.getRemainingTicks(stack, level));
            tooltips.add(Component.translatable("item.raven_dnd_origins.goodberry.tooltip.decays_in", remaining)
                    .withStyle(darkGrayItalic));
        }
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.GOODBERRY.get())) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && GoodberryItem.isDecayed(stack, level)) return;

        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        elements.add(1, Either.right(new HeartsTooltipComponent(HEARTS_COUNT)));
    }

    private static String formatTicksToTime(long ticks) {
        long seconds = ticks / 20;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m " + secs + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }
}
