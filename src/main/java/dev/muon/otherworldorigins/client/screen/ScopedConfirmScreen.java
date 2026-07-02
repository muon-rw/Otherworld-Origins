package dev.muon.otherworldorigins.client.screen;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.BeginReselectionMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Confirmation shown after a reselection (orb or single-layer re-pick): displays the same full
 * character-sheet prose as {@link FinalConfirmScreen} — including selections that weren't re-picked
 * this turn — with Confirm to keep them or Re-pick to clear the reselected layers and choose again.
 */
@OnlyIn(Dist.CLIENT)
public class ScopedConfirmScreen extends Screen {

    private static final ResourceLocation CHARACTER_SHEET = OtherworldOrigins.loc("textures/gui/character_sheet.png");
    private static final int SHEET_WIDTH = 256;
    private static final int SHEET_HEIGHT = 256;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 5;

    private final List<ResourceLocation> layers;
    private final CharacterSheetProse prose = new CharacterSheetProse();

    public ScopedConfirmScreen(List<ResourceLocation> layers) {
        super(Component.translatable("otherworldorigins.gui.scoped_confirm.title"));
        this.layers = layers;
    }

    @Override
    protected void init() {
        super.init();
        prose.rebuild(this.font, this.minecraft != null ? this.minecraft.player : null);

        int centerX = this.width / 2;
        int sheetY = (this.height - SHEET_HEIGHT) / 2;
        int defaultButtonY = sheetY + SHEET_HEIGHT + PADDING;
        int buttonY = defaultButtonY + BUTTON_HEIGHT + PADDING <= this.height
                ? defaultButtonY
                : this.height - BUTTON_HEIGHT - PADDING * 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("otherworldorigins.gui.scoped_confirm.confirm"),
                        b -> this.onClose())
                .bounds(centerX - BUTTON_WIDTH - PADDING + 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("otherworldorigins.gui.scoped_confirm.repick"),
                        b -> {
                            OtherworldOrigins.CHANNEL.sendToServer(new BeginReselectionMessage(this.layers));
                            this.onClose();
                        })
                .bounds(centerX + PADDING + 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int sheetX = (this.width - SHEET_WIDTH) / 2;
        int sheetY = (this.height - SHEET_HEIGHT) / 2;
        graphics.blit(CHARACTER_SHEET, sheetX, sheetY, 0, 0, SHEET_WIDTH, SHEET_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, sheetY - 20, 0xFFFFFF);
        prose.render(graphics, this.font, sheetX, sheetY + 40);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
