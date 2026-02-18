package dev.muon.otherworldorigins.client;

import dev.muon.otherworldorigins.item.HeartsTooltipComponent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders heart sprites in tooltips using the vanilla GUI icons texture.
 */
public class HeartsTooltipRenderer implements ClientTooltipComponent {

    private static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");

    private final int hearts;

    public HeartsTooltipRenderer(HeartsTooltipComponent component) {
        this.hearts = component.hearts();
    }

    @Override
    public int getHeight() {
        return 9;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(0, (hearts - 1) * 8) + 9; // Gui.HEART_SEPARATION, HEART_SIZE
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        int heartX = Gui.HeartType.NORMAL.getX(false, false);
        int heartY = 0; // Same as vanilla renderHearts: 9 * (hardcore ? 5 : 0)
        int heartSeparation = 8; // Gui.HEART_SEPARATION
        int renderY = y - 1; // Align with text baseline - tooltip image components render 1px low
        for (int i = 0; i < hearts; i++) {
            guiGraphics.blit(GUI_ICONS_LOCATION, x + i * heartSeparation, renderY, heartX, heartY, 9, 9);
        }
    }
}
