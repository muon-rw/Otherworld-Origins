package dev.muon.otherworldorigins.mixin.origins_patches;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.ResetOriginsMessage;
import dev.muon.otherworldorigins.util.ClientLayerScreenHelper;
import dev.muon.otherworldorigins.util.ElementalDisciplineSpellDisplay;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private List<Holder<OriginLayer>> layerList;
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

    @Inject(method = "Lio/github/apace100/origins/screen/ChooseOriginScreen;init()V", at = @At("TAIL"), require = 1)
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

    @Inject(method = "Lio/github/apace100/origins/screen/ChooseOriginScreen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"), require = 1)
    private void renderCurrentOrigins(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (otherworld$isFeatLayer() || otherworld$isWildshapeLayer()) return;

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

        IOriginContainer originContainer = IOriginContainer.get(scrn().getMinecraft().player).resolve().orElse(null);
        if (originContainer != null) {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
            Registry<Origin> originRegistry = OriginsAPI.getOriginsRegistry(null);

            // Render left column (Race, Subrace, Class, Subclass)
            int y = startY;
            for (ResourceLocation layerId : layerIds) {
                ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
                Holder<OriginLayer> layer = layerRegistry.getHolder(layerKey).orElse(null);

                if (layer != null) {
                    ResourceKey<Origin> originKey = originContainer.getOrigin(layer);
                    
                    // Only render if an origin is selected (not empty)
                    if (originKey != null && !originKey.location().equals(ResourceLocation.fromNamespaceAndPath("origins", "empty"))) {
                        Holder<Origin> origin = originRegistry.getHolder(originKey).orElse(null);
                        if (origin != null) {
                            Component layerName = layer.value().name().copy().withStyle(style -> style.withUnderlined(true));
                            Component originName = origin.value().getName();
                            
                            // Render layer name and origin name
                            graphics.drawString(scrn().getMinecraft().font, Component.translatable("otherworldorigins.gui.layer_origin", layerName, originName), startX, y, 0xFFFFFF);
                            
                            y += LINE_HEIGHT;
                        }
                    }
                }
            }

            // Render right column (Feats)
            List<Holder<Origin>> featOrigins = otherworld$getFeatOriginsForRendering(originContainer, layerRegistry, originRegistry);
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
                    Holder<Origin> originHolder = featOrigins.get(i);
                    int column = i % 2; // 0 for left column, 1 for right column
                    int row = i / 2;
                    
                    int iconX = column == 0 ? featX : column2X;
                    int iconY = featY + (row * 20); // Icon size (16) + spacing (4)
                    
                    otherworld$renderOriginIcon(graphics, originHolder, iconX, iconY, mouseX, mouseY);
                }
            }
        }
    }


    @Unique
    private boolean otherworld$isFeatLayer() {
        if (currentLayerIndex >= 0 && currentLayerIndex < layerList.size()) {
            Holder<OriginLayer> currentLayer = layerList.get(currentLayerIndex);
            ResourceLocation layerId = currentLayer.unwrapKey()
                    .map(ResourceKey::location)
                    .orElse(null);

            if (layerId != null) {
                return layerId.equals(OtherworldOrigins.loc("free_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("first_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("second_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("third_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("fourth_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("fifth_feat")) ||
                        layerId.equals(OtherworldOrigins.loc("elemental_discipline_one")) ||
                        layerId.equals(OtherworldOrigins.loc("elemental_discipline_two")) ||
                        layerId.equals(OtherworldOrigins.loc("elemental_discipline_three")) ||
                        layerId.equals(OtherworldOrigins.loc("elemental_discipline_four"));
            }
        }
        return false;
    }

    @Unique
    private boolean otherworld$isWildshapeLayer() {
        if (currentLayerIndex >= 0 && currentLayerIndex < layerList.size()) {
            Holder<OriginLayer> currentLayer = layerList.get(currentLayerIndex);
            ResourceLocation layerId = currentLayer.unwrapKey()
                    .map(ResourceKey::location)
                    .orElse(null);
            if (layerId != null) {
                return layerId.equals(OtherworldOrigins.loc("wildshape"));
            }
        }
        return false;
    }

    @Unique
    private boolean otherworld$shouldHideButton() {
        if (otherworld$isFeatLayer()) return true;
        if (otherworld$isWildshapeLayer()) return true;
        if (currentLayerIndex >= 0 && currentLayerIndex < layerList.size()) {
            Holder<OriginLayer> currentLayer = layerList.get(currentLayerIndex);
            ResourceLocation layerId = currentLayer.unwrapKey()
                    .map(ResourceKey::location)
                    .orElse(null);
            if (layerId != null) {
                return layerId.equals(OtherworldOrigins.loc("plus_one_aptitude_resilient"));
            }
        }
        return false;
    }

    @Unique
    private List<Holder<Origin>> otherworld$getFeatOriginsForRendering(IOriginContainer originContainer, Registry<OriginLayer> layerRegistry, Registry<Origin> originRegistry) {
        List<Holder<Origin>> featOrigins = new ArrayList<>();
        ResourceLocation[] featLayerIds = {
                OtherworldOrigins.loc("free_feat"),
                OtherworldOrigins.loc("first_feat"),
                OtherworldOrigins.loc("second_feat"),
                OtherworldOrigins.loc("third_feat"),
                OtherworldOrigins.loc("fourth_feat"),
                OtherworldOrigins.loc("fifth_feat"),
                OtherworldOrigins.loc("elemental_discipline_one"),
                OtherworldOrigins.loc("elemental_discipline_two"),
                OtherworldOrigins.loc("elemental_discipline_three"),
                OtherworldOrigins.loc("elemental_discipline_four")
        };

        for (ResourceLocation layerId : featLayerIds) {
            ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
            Holder<OriginLayer> layer = layerRegistry.getHolder(layerKey).orElse(null);

            if (layer != null) {
                ResourceKey<Origin> originKey = originContainer.getOrigin(layer);
                if (originKey != null && !originKey.location().equals(ResourceLocation.fromNamespaceAndPath("origins", "empty"))) {
                    Holder<Origin> origin = originRegistry.getHolder(originKey).orElse(null);
                    if (origin != null) {
                        featOrigins.add(origin);
                    }
                }
            }
        }

        return featOrigins;
    }
    
    @Unique
    private void otherworld$renderOriginIcon(GuiGraphics graphics, Holder<Origin> originHolder, int x, int y, int mouseX, int mouseY) {
        Origin origin = originHolder.value();
        String originPath = originHolder.unwrapKey().map(key -> key.location().getPath()).orElse("");
        Optional<ResourceLocation> disciplineSpell = ElementalDisciplineSpellDisplay.spellIdForDisciplineOriginPath(originPath);
        if (disciplineSpell.isPresent()) {
            ResourceLocation id = disciplineSpell.get();
            ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath(
                    id.getNamespace(), "textures/gui/spell_icons/" + id.getPath() + ".png");
            graphics.blit(iconTexture, x, y, 0, 0, 16, 16, 16, 16);
        } else {
            graphics.renderItem(origin.getIcon(), x, y);
        }

        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            graphics.renderTooltip(scrn().getMinecraft().font, origin.getName(), mouseX, mouseY);
        }
    }

    @Unique
    private static final Set<ResourceLocation> CANTRIP_GRANTING_SUBCLASSES = Set.of(
            OtherworldOrigins.loc("subclass/rogue/arcane_trickster"),
            OtherworldOrigins.loc("subclass/fighter/eldritch_knight")
    );

    @ModifyReturnValue(
            method = "getTitleText()Lnet/minecraft/network/chat/Component;",
            at = @At("RETURN"),
            require = 1,
            remap = false
    )
    private Component otherworld$injectCantripSourceName(Component original) {
        if (currentLayerIndex < 0 || currentLayerIndex >= layerList.size()) return original;

        Holder<OriginLayer> currentLayer = layerList.get(currentLayerIndex);
        ResourceLocation layerId = currentLayer.unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);
        if (layerId == null) return original;

        IOriginContainer container = IOriginContainer.get(scrn().getMinecraft().player).resolve().orElse(null);
        if (container == null) return original;

        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
        Registry<Origin> originRegistry = OriginsAPI.getOriginsRegistry(null);

        String sourceName = null;

        if (layerId.equals(OtherworldOrigins.loc("wildshape"))) {
            return Component.translatable("otherworldorigins.gui.wildshape_choose_title");
        }

        if (layerId.equals(OtherworldOrigins.loc("cantrip_one"))) {
            sourceName = otherworld$getOriginName(container, layerRegistry, originRegistry, OtherworldOrigins.loc("subrace"));
        } else if (layerId.equals(OtherworldOrigins.loc("cantrip_two"))) {
            sourceName = otherworld$getCantripTwoSourceName(container, layerRegistry, originRegistry);
        }

        if (sourceName != null) {
            return Component.translatable("otherworldorigins.gui.cantrip_choose_title", sourceName);
        }

        return original;
    }

    @Unique
    private String otherworld$getCantripTwoSourceName(IOriginContainer container, Registry<OriginLayer> layerRegistry, Registry<Origin> originRegistry) {
        ResourceKey<OriginLayer> subclassKey = ResourceKey.create(layerRegistry.key(), OtherworldOrigins.loc("subclass"));
        Holder<OriginLayer> subclassLayer = layerRegistry.getHolder(subclassKey).orElse(null);

        if (subclassLayer != null) {
            ResourceKey<Origin> originKey = container.getOrigin(subclassLayer);
            if (originKey != null && CANTRIP_GRANTING_SUBCLASSES.contains(originKey.location())) {
                Holder<Origin> origin = originRegistry.getHolder(originKey).orElse(null);
                if (origin != null) {
                    return origin.value().getName().getString();
                }
            }
        }

        return otherworld$getOriginName(container, layerRegistry, originRegistry, OtherworldOrigins.loc("class"));
    }

    @Unique
    private String otherworld$getOriginName(IOriginContainer container, Registry<OriginLayer> layerRegistry, Registry<Origin> originRegistry, ResourceLocation layerId) {
        ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
        Holder<OriginLayer> layer = layerRegistry.getHolder(layerKey).orElse(null);

        if (layer != null) {
            ResourceKey<Origin> originKey = container.getOrigin(layer);
            if (originKey != null && !originKey.location().equals(ResourceLocation.fromNamespaceAndPath("origins", "empty"))) {
                Holder<Origin> origin = originRegistry.getHolder(originKey).orElse(null);
                if (origin != null) {
                    return origin.value().getName().getString();
                }
            }
        }
        return null;
    }
}