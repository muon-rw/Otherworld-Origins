package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientHandler {

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
                    ChooseOriginScreen newScreen = new ChooseOriginScreen(featOriginLayers, 0, false);
                    minecraft.setScreen(newScreen);
                });
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void handleValidatedLayers(List<ResourceLocation> validMissingLayers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
            List<Holder<OriginLayer>> missingOriginLayers = new ArrayList<>();

            for (ResourceLocation layerId : validMissingLayers) {
                OriginLayer layer = layerRegistry.get(layerId);
                if (layer != null) {
                    missingOriginLayers.add(layerRegistry.getHolderOrThrow(layerRegistry.getResourceKey(layer).orElseThrow()));
                }
            }

            if (!missingOriginLayers.isEmpty()) {
                OtherworldOrigins.LOGGER.info("Reopening selection screen for validated layers:");
                for (Holder<OriginLayer> layerHolder : missingOriginLayers) {
                    OtherworldOrigins.LOGGER.info("- " + layerHolder.value().name().getString());
                }
                minecraft.execute(() -> {
                    ChooseOriginScreen newScreen = new ChooseOriginScreen(missingOriginLayers, 0, false);
                    minecraft.setScreen(newScreen);
                });
            }
        }
    }
}