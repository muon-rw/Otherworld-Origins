package dev.muon.raven_dnd_origins.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class DeskButton extends Button {

    /** Set on the button that commits the session; we light its plate and slowly pulse its label. */
    private boolean emphasized;

    public DeskButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public void setEmphasized(boolean emphasized) {
        this.emphasized = emphasized;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean lit = this.active && (this.isHoveredOrFocused() || this.emphasized);
        graphics.fill(getX() + 2, getY() + 2, getX() + width + 2, getY() + height + 2, DeskStyle.SHADOW);
        graphics.blitSprite(!this.active ? DeskStyle.BUTTON_DISABLED : lit ? DeskStyle.BUTTON_HIGHLIGHTED : DeskStyle.BUTTON,
                getX(), getY(), width, height);
        int color = !this.active ? DeskStyle.MUTED : lit ? DeskStyle.CREAM : DeskStyle.GILT;
        if (this.active && this.emphasized && !this.isHoveredOrFocused()) {
            float breath = 0.5f + 0.5f * Mth.sin(Util.getMillis() / 450f);
            color = DeskStyle.lerpColor(breath, DeskStyle.GILT, DeskStyle.CREAM);
        }
        renderScrollingString(graphics, Minecraft.getInstance().font, 4, color);
    }
}
