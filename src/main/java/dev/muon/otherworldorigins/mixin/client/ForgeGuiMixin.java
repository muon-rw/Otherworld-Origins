package dev.muon.otherworldorigins.mixin.client;

import dev.muon.otherworldorigins.power.UndeadVitalsPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ForgeGui.class, remap = false)
public class ForgeGuiMixin {

    @Inject(method = "renderAir(IILnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$hideAirBarForUndead(int width, int height, GuiGraphics graphics, CallbackInfo ci) {
        if (UndeadVitalsPower.has(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }
}
