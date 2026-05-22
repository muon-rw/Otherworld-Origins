package dev.muon.otherworldorigins.client.screen;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.BeginReselectionMessage;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Lighter confirmation shown after a reselection (orb or single-layer re-pick): summarizes only
 * the layers that changed, with Confirm to keep them or Re-pick to clear and choose again.
 */
@OnlyIn(Dist.CLIENT)
public class ScopedConfirmScreen extends Screen {

    private static final ResourceLocation CHARACTER_SHEET = OtherworldOrigins.loc("textures/gui/character_sheet.png");
    private static final ResourceLocation EMPTY_ORIGIN = ResourceLocation.fromNamespaceAndPath("origins", "empty");
    private static final int SHEET_WIDTH = 256;
    private static final int SHEET_HEIGHT = 256;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 14;

    private final List<ResourceLocation> layers;
    private final List<Component> lines = new ArrayList<>();

    public ScopedConfirmScreen(List<ResourceLocation> layers) {
        super(Component.translatable("otherworldorigins.gui.scoped_confirm.title"));
        this.layers = layers;
    }

    @Override
    protected void init() {
        super.init();
        buildLines();

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

    private void buildLines() {
        lines.clear();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        IOriginContainer container = IOriginContainer.get(minecraft.player).resolve().orElse(null);
        if (container == null) {
            return;
        }
        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
        Registry<Origin> originRegistry = OriginsAPI.getOriginsRegistry(null);

        for (ResourceLocation layerId : layers) {
            Holder<OriginLayer> layer = layerRegistry
                    .getHolder(ResourceKey.create(layerRegistry.key(), layerId))
                    .orElse(null);
            if (layer == null) {
                continue;
            }
            ResourceKey<Origin> originKey = container.getOrigin(layer);
            if (originKey == null || originKey.location().equals(EMPTY_ORIGIN)) {
                continue;
            }
            Holder<Origin> origin = originRegistry.getHolder(originKey).orElse(null);
            if (origin == null) {
                continue;
            }
            Component layerName = layer.value().name().copy().withStyle(ChatFormatting.GRAY);
            lines.add(Component.translatable("otherworldorigins.gui.layer_origin", layerName, origin.value().getName()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int sheetX = (this.width - SHEET_WIDTH) / 2;
        int sheetY = (this.height - SHEET_HEIGHT) / 2;
        graphics.blit(CHARACTER_SHEET, sheetX, sheetY, 0, 0, SHEET_WIDTH, SHEET_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, sheetY - 20, 0xFFFFFF);
        int y = sheetY + 60;
        for (Component line : lines) {
            graphics.drawCenteredString(this.font, line, this.width / 2, y, 0x3F3F3F);
            y += LINE_HEIGHT;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
