package dev.muon.raven_dnd_origins.mixin.client;

import dev.muon.raven_dnd_origins.power.HungerImmunityPower;
import dev.muon.raven_dnd_origins.power.SuffocationImmunityPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "renderAirLevel(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$hideAirBarForUndead(GuiGraphics graphics, CallbackInfo ci) {
        if (SuffocationImmunityPower.has(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFoodLevel(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$hideFoodBarForHungerImmune(GuiGraphics graphics, CallbackInfo ci) {
        if (HungerImmunityPower.has(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }
}
