package dev.muon.otherworldorigins.mixin.origins_patches;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.RequestLayerValidationMessage;
import dev.muon.otherworldorigins.restrictions.SpellRestrictions;
import dev.muon.otherworldorigins.util.ClientLayerScreenHelper;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.screen.WaitForNextLayerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

@Mixin(value = WaitForNextLayerScreen.class, remap = false)
public class WaitForNextLayerScreenMixin {
    @Shadow(remap = false)
    @Final
    private ArrayList<OriginLayer> layerList;
    
    @Inject(method = "openSelection()V", at = @At("HEAD"))
    private void onOpenSelectionStart(CallbackInfo ci) {
        // Clear tracked layers if this is a fresh start (race layer is in the list)
        if (layerList != null) {
            for (OriginLayer layer : layerList) {
                ResourceLocation layerId = layer.getIdentifier();
                if (layerId != null && layerId.equals(OtherworldOrigins.loc("race"))) {
                    ClientLayerScreenHelper.clearSelectedLayers();
                    break;
                }
            }
        }
    }
    
    @Inject(method = "openSelection()V", at = @At(value = "RETURN", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", remap = true, ordinal = 1))
    private void onOpenSelectionEnd(CallbackInfo ci) {
        OtherworldOrigins.LOGGER.info("Origin Selection is about to close, sending validation request to server");
        
        // Track which layers were part of this selection session
        Set<ResourceLocation> selectedLayerIds = new HashSet<>();
        if (layerList != null) {
            for (OriginLayer layer : layerList) {
                selectedLayerIds.add(layer.getIdentifier());
            }
        }
        ClientLayerScreenHelper.addToSelectedLayers(selectedLayerIds);
        
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.tell(() -> {
            OtherworldOrigins.CHANNEL.sendToServer(new RequestLayerValidationMessage());
        });
        if (minecraft.player != null) {
            SpellRestrictions.clearCache(minecraft.player);
        }
    }
}
