package dev.muon.otherworldorigins.mixin.origins_patches;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.RequestLayerValidationMessage;
import dev.muon.otherworldorigins.restrictions.SpellRestrictions;
import io.github.apace100.origins.screen.WaitForNextLayerScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WaitForNextLayerScreen.class)
public class WaitForNextLayerScreenMixin {
    @Inject(method = "openSelection", at = @At(value = "RETURN", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", ordinal = 2))
    private void onOpenSelectionEnd(CallbackInfo ci) {
        OtherworldOrigins.LOGGER.info("Origin Selection is about to close, sending validation request to server");
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.tell(() -> {
            OtherworldOrigins.CHANNEL.sendToServer(new RequestLayerValidationMessage());
        });
        if (minecraft.player != null) {
            SpellRestrictions.clearCache(minecraft.player);
        }
    }
}