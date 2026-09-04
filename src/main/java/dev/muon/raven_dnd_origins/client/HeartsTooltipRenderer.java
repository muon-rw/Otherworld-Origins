package dev.muon.raven_dnd_origins.client;

import dev.muon.raven_dnd_origins.item.HeartsTooltipComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders heart sprites in tooltips using the vanilla HUD heart sprites.
 */
public class HeartsTooltipRenderer implements ClientTooltipComponent {

    private static final int HEART_SIZE = 9;
    private static final int HEART_SEPARATION = 8;

    private final int hearts;

    public HeartsTooltipRenderer(HeartsTooltipComponent component) {
        this.hearts = component.hearts();
    }

    @Override
    public int getHeight() {
        return HEART_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(0, (hearts - 1) * HEART_SEPARATION) + HEART_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        ResourceLocation sprite = Gui.HeartType.NORMAL.getSprite(false, false, false);
        int renderY = y - 1; // tooltip image components render 1px low relative to the text baseline
        for (int i = 0; i < hearts; i++) {
            guiGraphics.blitSprite(sprite, x + i * HEART_SEPARATION, renderY, HEART_SIZE, HEART_SIZE);
        }
    }
}
