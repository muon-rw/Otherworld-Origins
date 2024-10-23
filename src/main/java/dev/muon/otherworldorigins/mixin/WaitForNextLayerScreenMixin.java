package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.screen.WaitForNextLayerScreen;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(WaitForNextLayerScreen.class)
public class WaitForNextLayerScreenMixin {
    @Shadow @Final private List<Holder<OriginLayer>> layerList;
    @Shadow @Final private boolean showDirtBackground;

    @Unique
    private static final Set<ResourceLocation> otherworld$excludedLayers = Set.of(
            OtherworldOrigins.loc("first_feat"),
            OtherworldOrigins.loc("second_feat"),
            OtherworldOrigins.loc("third_feat"),
            OtherworldOrigins.loc("fourth_feat"),
            OtherworldOrigins.loc("fifth_feat")
    );

    @Unique
    private static final Map<ResourceLocation, Long> otherworld$recentlySelectedLayers = new HashMap<>();

    @Unique
    private static final long COOLDOWN_TICKS = 100; // before reprompting a layer which was just selected

    @Unique
    private static int otherworld$checkAttempts = 0;

    @Unique
    private static final int MAX_CHECK_ATTEMPTS = 2;

    @Inject(method = "openSelection", at = @At(value = "RETURN", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal= 2))
    private void onOpenSelectionEnd(CallbackInfo ci) {
        OtherworldOrigins.LOGGER.debug("WaitForNextLayerScreen is about to close");
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.tell(() -> {
            minecraft.execute(this::otherworld$checkAndOpenMissingOriginScreen);
        });
    }

    @Unique
    private void otherworld$checkAndOpenMissingOriginScreen() {
        if (otherworld$checkAttempts >= MAX_CHECK_ATTEMPTS) {
            OtherworldOrigins.LOGGER.warn("Maximum check attempts reached. Stopping fallback reselector.");
            otherworld$checkAttempts = 0;
            return;
        }

        otherworld$checkAttempts++;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        IOriginContainer originContainer = IOriginContainer.get(minecraft.player).resolve().orElse(null);
        if (originContainer == null) {
            return;
        }

        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
        List<Holder<OriginLayer>> missingOriginLayers = new ArrayList<>();
        long currentTime = minecraft.level.getGameTime();

        for (OriginLayer layer : layerRegistry) {
            ResourceLocation layerId = layerRegistry.getKey(layer);
            if (layerId == null || otherworld$excludedLayers.contains(layerId)) {
                continue;
            }

            if (otherworld$recentlySelectedLayers.containsKey(layerId) &&
                    (currentTime - otherworld$recentlySelectedLayers.get(layerId) < COOLDOWN_TICKS)) {
                continue;
            }

            Holder<OriginLayer> layerHolder = layerRegistry.getHolderOrThrow(layerRegistry.getResourceKey(layer).orElseThrow());
            ResourceKey<Origin> originKey = originContainer.getOrigin(layerHolder);

            if (originKey == null || originKey.location().equals(new ResourceLocation("origins", "empty"))) {
                Set<Holder<Origin>> availableOrigins = layer.origins(minecraft.player);

                if (!availableOrigins.isEmpty()) {
                    missingOriginLayers.add(layerHolder);
                    otherworld$recentlySelectedLayers.put(layerId, currentTime);
                }
            }
        }

        if (!missingOriginLayers.isEmpty()) {
            OtherworldOrigins.LOGGER.info("Detected unpicked origins. Reopening selection screen for layers:");
            for (Holder<OriginLayer> layerHolder : missingOriginLayers) {
                OtherworldOrigins.LOGGER.info("- " + layerHolder.value().name().getString());
            }
            minecraft.execute(() -> {
                ChooseOriginScreen newScreen = new ChooseOriginScreen(missingOriginLayers, 0, this.showDirtBackground);
                minecraft.setScreen(newScreen);
            });
        } else {
            otherworld$checkAttempts = 0;
        }
    }
}