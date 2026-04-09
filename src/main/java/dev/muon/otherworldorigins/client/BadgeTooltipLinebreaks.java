package dev.muon.otherworldorigins.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Origins badge {@code addLines} uses one {@link ClientTextTooltip} when measured width fits
 * {@code widthLimit}. That path uses {@link Component#getVisualOrderText()}, which keeps line-feed
 * code points in a single sequence; tooltip rendering draws them as a missing-glyph box.
 * {@link Font#split} uses StringSplitter and breaks real newlines into separate tooltip rows.
 */
public final class BadgeTooltipLinebreaks {
    private BadgeTooltipLinebreaks() {}

    public static void addLines(List<ClientTooltipComponent> tooltips, Component text, Font textRenderer, int widthLimit) {
        if (containsLineBreakChar(text) || textRenderer.width(text) > widthLimit) {
            for (FormattedCharSequence line : textRenderer.split(text, widthLimit)) {
                tooltips.add(new ClientTextTooltip(line));
            }
        } else {
            tooltips.add(new ClientTextTooltip(text.getVisualOrderText()));
        }
    }

    private static boolean containsLineBreakChar(Component text) {
        return text.getString().indexOf('\n') >= 0;
    }
}
