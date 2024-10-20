package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.ResetOriginsMessage;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ChooseOriginScreen.class, remap = false)
public class ChooseOriginScreenMixin extends OriginDisplayScreen {

    @Unique
    private static final int MARGIN = 10;
    @Unique
    private static final int LINE_HEIGHT = 15;
    @Unique
    private static final int BUTTON_WIDTH = 100;
    @Unique
    private static final int BUTTON_HEIGHT = 20;

    public ChooseOriginScreenMixin(Component title, boolean showDirtBackground) {
        super(title, showDirtBackground);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addResetButton(CallbackInfo ci) {
        ChooseOriginScreen screen = (ChooseOriginScreen) (Object) this;
        addRenderableWidget(Button.builder(Component.translatable("otherworldorigins.gui.start_over"), (button) -> {
            OtherworldOrigins.CHANNEL.sendToServer(new ResetOriginsMessage());
            screen.onClose();
        }).bounds(MARGIN, screen.height - BUTTON_HEIGHT - MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCurrentOrigins(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChooseOriginScreen screen = (ChooseOriginScreen) (Object) this;
        int x = MARGIN;
        int y = MARGIN;

        Component currentSelectionsText = Component.translatable("otherworldorigins.gui.current_selections").withStyle(style -> style.withBold(true));
        graphics.drawString(screen.getMinecraft().font, currentSelectionsText, x, y, 0xFFFFFF);
        y += LINE_HEIGHT;

        ResourceLocation[] layerIds = {
                OtherworldOrigins.loc("race"),
                OtherworldOrigins.loc("subrace"),
                OtherworldOrigins.loc("class"),
                OtherworldOrigins.loc("subclass")
        };

        IOriginContainer originContainer = IOriginContainer.get(screen.getMinecraft().player).resolve().orElse(null);
        if (originContainer != null) {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
            Registry<Origin> originRegistry = OriginsAPI.getOriginsRegistry(null);

            for (ResourceLocation layerId : layerIds) {
                ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
                Holder<OriginLayer> layer = layerRegistry.getHolder(layerKey).orElse(null);

                if (layer != null) {
                    ResourceKey<Origin> originKey = originContainer.getOrigin(layer);
                    Component originName = Component.translatable("origins.origin.empty");

                    if (originKey != null) {
                        Holder<Origin> origin = originRegistry.getHolder(originKey).orElse(null);
                        if (origin != null) {
                            originName = origin.value().getName();
                        }
                    }

                    Component layerName = layer.value().name();
                    graphics.drawString(screen.getMinecraft().font, Component.translatable("otherworldorigins.gui.layer_origin", layerName, originName), x, y, 0xFFFFFF);
                    y += LINE_HEIGHT;
                }
            }

            // Render Feats
            List<Component> feats = getFeats(originContainer, layerRegistry, originRegistry);
            if (!feats.isEmpty()) {
                y += LINE_HEIGHT; // Add some space before Feats section
                Component featsText = Component.translatable("otherworldorigins.gui.feats").withStyle(style -> style.withBold(true));
                graphics.drawString(screen.getMinecraft().font, featsText, x, y, 0xFFFFFF);
                y += LINE_HEIGHT;

                for (Component feat : feats) {
                    graphics.drawString(screen.getMinecraft().font, feat, x, y, 0xFFFFFF);
                    y += LINE_HEIGHT;
                }
            }
        }
    }

    @Unique
    private List<Component> getFeats(IOriginContainer originContainer, Registry<OriginLayer> layerRegistry, Registry<Origin> originRegistry) {
        List<Component> feats = new ArrayList<>();
        ResourceLocation[] featLayerIds = {
                OtherworldOrigins.loc("free_feat"),
                OtherworldOrigins.loc("first_feat"),
                OtherworldOrigins.loc("second_feat"),
                OtherworldOrigins.loc("third_feat"),
                OtherworldOrigins.loc("fourth_feat"),
                OtherworldOrigins.loc("fifth_feat")
        };

        for (ResourceLocation layerId : featLayerIds) {
            ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
            Holder<OriginLayer> layer = layerRegistry.getHolder(layerKey).orElse(null);

            if (layer != null) {
                ResourceKey<Origin> originKey = originContainer.getOrigin(layer);
                if (originKey != null && !originKey.location().equals(new ResourceLocation("origins", "empty"))) {
                    Holder<Origin> origin = originRegistry.getHolder(originKey).orElse(null);
                    if (origin != null) {
                        feats.add(origin.value().getName());
                    }
                }
            }
        }

        return feats;
    }
}