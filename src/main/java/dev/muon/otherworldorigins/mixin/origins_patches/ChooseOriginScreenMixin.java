package dev.muon.otherworldorigins.mixin.origins_patches;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.ResetOriginsMessage;
import dev.muon.otherworldorigins.util.ClientLayerScreenHelper;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ChooseOriginScreen.class)
public class ChooseOriginScreenMixin extends OriginDisplayScreen {

    @Unique
    private static final int MARGIN = 10;
    @Unique
    private static final int LINE_HEIGHT = 14;
    @Unique
    private static final int BUTTON_WIDTH = 100;
    @Unique
    private static final int BUTTON_HEIGHT = 20;

    @Final
    @Shadow(remap = false)
    private ArrayList<OriginLayer> layerList;
    @Final
    @Shadow(remap = false)
    private int currentLayerIndex;

    @Unique
    private ChooseOriginScreen scrn() {
        return (ChooseOriginScreen) (Object) this;
    }

    public ChooseOriginScreenMixin(Component title, boolean showDirtBackground) {
        super(title, showDirtBackground);
    }

    @Inject(method = "init()V", at = @At("TAIL"), require = 1)
    private void addResetButton(CallbackInfo ci) {
        if (otherworld$shouldHideButton()) {
            return;
        }
        this.addRenderableWidget(Button.builder(Component.translatable("otherworldorigins.gui.start_over"), (button) -> {
            ClientLayerScreenHelper.clearSelectedLayers(); // Clear tracked layers when starting over
            OtherworldOrigins.CHANNEL.sendToServer(new ResetOriginsMessage());
            scrn().onClose();
        }).bounds(MARGIN, scrn().height - BUTTON_HEIGHT - MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"), require = 1)
    private void renderCurrentOrigins(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Calculate starting position - center of screen, 5px down, 180px to the left
        int centerX = scrn().width / 2;
        int centerY = scrn().height / 2;
        int startX = centerX - 189;
        int startY = centerY + 9;

        ResourceLocation[] layerIds = {
                OtherworldOrigins.loc("race"),
                OtherworldOrigins.loc("subrace"),
                OtherworldOrigins.loc("class"),
                OtherworldOrigins.loc("subclass")
        };

        OriginComponent originComponent = ModComponents.ORIGIN.maybeGet(scrn().getMinecraft().player).orElse(null);
        if (originComponent != null) {
            // Render left column (Race, Subrace, Class, Subclass)
            int y = startY;
            for (ResourceLocation layerId : layerIds) {
                try {
                    OriginLayer layer = null;//OriginLayers.getLayer(layerId);
                    Origin origin = originComponent.getOrigin(layer);
                    
                    // Only render if an origin is selected (not empty)
                    if (origin != null && origin != Origin.EMPTY) {
                        Component layerName = Component.translatable(layer.getTranslationKey()).copy().withStyle(style -> style.withUnderlined(true));
                        Component originName = origin.getName();
                        
                        // Render layer name and origin name
                        graphics.drawString(scrn().getMinecraft().font, Component.translatable("otherworldorigins.gui.layer_origin", layerName, originName), startX, y, 0xFFFFFF);
                        
                        y += LINE_HEIGHT;
                    }
                } catch (IllegalArgumentException e) {
                    // Layer doesn't exist, skip it
                }
            }

            // Render right column (Feats)
            List<Origin> featOrigins = otherworld$getFeatOriginsForRendering(originComponent);
            if (!featOrigins.isEmpty()) {
                int featX = centerX - 52;
                int featY = startY - LINE_HEIGHT;
                
                // Render "Feats:" header with underline
                Component featsText = Component.translatable("otherworldorigins.gui.feats").withStyle(style -> style.withUnderlined(true));
                graphics.drawString(scrn().getMinecraft().font, featsText, featX + 3, featY, 0xFFFFFF);
                featY += LINE_HEIGHT + 1; // Extra spacing after header

                // Render feat icons in 2 columns
                int column2X = featX + 20;
                for (int i = 0; i < featOrigins.size(); i++) {
                    Origin origin = featOrigins.get(i);
                    int column = i % 2; // 0 for left column, 1 for right column
                    int row = i / 2;
                    
                    int iconX = column == 0 ? featX : column2X;
                    int iconY = featY + (row * 20); // Icon size (16) + spacing (4)
                    
                    otherworld$renderOriginIcon(graphics, origin, iconX, iconY, mouseX, mouseY);
                }
            }
        }
    }


    @Unique
    private boolean otherworld$shouldHideButton() {
        if (currentLayerIndex >= 0 && currentLayerIndex < layerList.size()) {
            OriginLayer currentLayer = layerList.get(currentLayerIndex);
            ResourceLocation layerId = currentLayer.getIdentifier();

            if (layerId != null) {
                return layerId.equals(OtherworldOrigins.loc("first_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("second_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("third_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("fourth_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("fifth_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("plus_one_aptitude_resilient"));
            }
        }
        return false;
    }

    @Unique
    private List<Origin> otherworld$getFeatOriginsForRendering(OriginComponent originComponent) {
        List<Origin> featOrigins = new ArrayList<>();
        ResourceLocation[] featLayerIds = {
                OtherworldOrigins.loc("free_feat"),
                OtherworldOrigins.loc("first_feat"),
                OtherworldOrigins.loc("second_feat"),
                OtherworldOrigins.loc("third_feat"),
                OtherworldOrigins.loc("fourth_feat"),
                OtherworldOrigins.loc("fifth_feat")
        };

        for (ResourceLocation layerId : featLayerIds) {
            try {
                OriginLayer layer = OriginLayers.getLayer(layerId);
                Origin origin = originComponent.getOrigin(layer);
                if (origin != null && origin != Origin.EMPTY) {
                    featOrigins.add(origin);
                }
            } catch (IllegalArgumentException e) {
                // Layer doesn't exist, skip it
            }
        }

        return featOrigins;
    }
    
    @Unique
    private void otherworld$renderOriginIcon(GuiGraphics graphics, Origin origin, int x, int y, int mouseX, int mouseY) {
        ItemStack icon = origin.getDisplayItem();
        
        // Render the icon
        graphics.renderItem(icon, x, y);
        
        // Check if mouse is hovering over the icon
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            // Render tooltip with feat name
            graphics.renderTooltip(scrn().getMinecraft().font, origin.getName(), mouseX, mouseY);
        }
    }
}
