package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.client.screen.FinalConfirmScreen;
import dev.muon.otherworldorigins.client.screen.OtherworldOriginScreen;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@OnlyIn(Dist.CLIENT)
public class ClientLayerScreenHelper {
    private static int validationAttempts = 0;
    private static final int MAX_VALIDATION_ATTEMPTS = 20;
    private static final Set<ResourceLocation> lastSelectedLayers = new HashSet<>();

    @OnlyIn(Dist.CLIENT)
    public static void handleValidatedLayers(List<ResourceLocation> validMissingLayers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            validationAttempts++;

            if (validationAttempts >= MAX_VALIDATION_ATTEMPTS) {
                resetValidationAttempts();
                minecraft.execute(() -> {
                    minecraft.player.connection.getConnection().disconnect(
                            Component.translatable("otherworldorigins.disconnect.validation_failed")
                    );
                });
                return;
            }

            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);

            Set<ResourceLocation> missingSet = new HashSet<>(validMissingLayers);
            boolean hasMissing = false;
            for (ResourceLocation layerId : validMissingLayers) {
                if (layerRegistry.get(layerId) != null) {
                    hasMissing = true;
                    break;
                }
            }

            if (hasMissing) {
                List<Holder<OriginLayer>> allActiveLayers = new ArrayList<>();
                for (Holder.Reference<OriginLayer> ref : OriginsAPI.getActiveLayers()) {
                    allActiveLayers.add(ref);
                }
                allActiveLayers.sort(Comparator.comparing(Holder::value));

                OtherworldOrigins.LOGGER.info("Reopening selection screen with all layers. Missing layers:");
                for (ResourceLocation id : missingSet) {
                    OtherworldOrigins.LOGGER.info("- " + id);
                }
                minecraft.execute(() -> {
                    OtherworldOriginScreen newScreen = new OtherworldOriginScreen(allActiveLayers, 0, false, false);
                    minecraft.setScreen(newScreen);
                });
            }
        }
    }
    public static void resetValidationAttempts() {
        validationAttempts = 0;
    }

    @OnlyIn(Dist.CLIENT)
    public static void setFinalConfirmScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new FinalConfirmScreen());
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleFeatLayers(List<ResourceLocation> validLayerIds) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
            List<Holder<OriginLayer>> featOriginLayers = new ArrayList<>();

            for (ResourceLocation layerId : validLayerIds) {
                OriginLayer layer = layerRegistry.get(layerId);
                if (layer != null) {
                    featOriginLayers.add(layerRegistry.getHolderOrThrow(layerRegistry.getResourceKey(layer).orElseThrow()));
                }
            }

            if (!featOriginLayers.isEmpty()) {
                OtherworldOrigins.LOGGER.debug("Opening selection screen for feat layers:");
                for (Holder<OriginLayer> layerHolder : featOriginLayers) {
                    OtherworldOrigins.LOGGER.debug("- " + layerHolder.value().name().getString());
                }
                minecraft.execute(() -> {
                    OtherworldOriginScreen newScreen = new OtherworldOriginScreen(featOriginLayers, 0, false, true);
                    minecraft.setScreen(newScreen);
                });
            }
        }
    }
    
    public static void addToSelectedLayers(Set<ResourceLocation> layers) {
        lastSelectedLayers.addAll(layers);
        OtherworldOrigins.LOGGER.debug("Added layers to tracking: {}. Total tracked: {}", layers, lastSelectedLayers);
    }
    
    public static void clearSelectedLayers() {
        OtherworldOrigins.LOGGER.debug("Clearing tracked layers. Previous: {}", lastSelectedLayers);
        lastSelectedLayers.clear();
    }
    
    public static boolean wasOnlyDynamicLayersSelected() {
        if (lastSelectedLayers.isEmpty()) {
            return false;
        }
        
        Set<ResourceLocation> dynamicLayerIds = Set.of(
                OtherworldOrigins.loc("first_feat"), OtherworldOrigins.loc("second_feat"),
                OtherworldOrigins.loc("third_feat"), OtherworldOrigins.loc("fourth_feat"),
                OtherworldOrigins.loc("fifth_feat"),
                OtherworldOrigins.loc("plus_one_aptitude_resilient"), OtherworldOrigins.loc("wildshape")
        );
        
        for (ResourceLocation layerId : lastSelectedLayers) {
            if (!dynamicLayerIds.contains(layerId)) {
                return false;
            }
        }
        
        return true;
    }
}