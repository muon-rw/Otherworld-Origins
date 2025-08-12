package dev.muon.otherworldorigins.mixin.origins_patches;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.RequestLayerValidationMessage;
import dev.muon.otherworldorigins.restrictions.SpellRestrictions;
import dev.muon.otherworldorigins.util.ClientLayerScreenHelper;
import io.github.apace100.origins.screen.WaitForNextLayerScreen;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Mixin(value = WaitForNextLayerScreen.class, remap = false)
public class WaitForNextLayerScreenMixin {
    @Shadow(remap = false)
    @Final
    private List<Holder<OriginLayer>> layerList;
    
    @Inject(method = "Lio/github/apace100/origins/screen/WaitForNextLayerScreen;openSelection()V", at = @At("HEAD"))
    private void onOpenSelectionStart(CallbackInfo ci) {
        // Clear tracked layers if this is a fresh start (race layer is in the list)
        if (layerList != null) {
            for (Holder<OriginLayer> layerHolder : layerList) {
                ResourceLocation layerId = layerHolder.unwrapKey().map(key -> key.location()).orElse(null);
                if (layerId != null && layerId.equals(OtherworldOrigins.loc("race"))) {
                    ClientLayerScreenHelper.clearSelectedLayers();
                    break;
                }
            }
        }
    }
    
    @Inject(method = "Lio/github/apace100/origins/screen/WaitForNextLayerScreen;openSelection()V", at = @At(value = "RETURN", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", remap = true, ordinal = 2))
    private void onOpenSelectionEnd(CallbackInfo ci) {
        OtherworldOrigins.LOGGER.info("Origin Selection is about to close, sending validation request to server");
        
        // Track which layers were part of this selection session
        Set<ResourceLocation> selectedLayerIds = new HashSet<>();
        if (layerList != null) {
            for (Holder<OriginLayer> layerHolder : layerList) {
                layerHolder.unwrapKey().ifPresent(key -> selectedLayerIds.add(key.location()));
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