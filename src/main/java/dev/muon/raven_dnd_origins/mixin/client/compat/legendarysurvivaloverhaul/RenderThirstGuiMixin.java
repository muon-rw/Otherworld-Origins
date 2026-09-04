package dev.muon.raven_dnd_origins.mixin.client.compat.legendarysurvivaloverhaul;

import dev.muon.raven_dnd_origins.power.ThirstImmunityPower;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sfiomn.legendarysurvivaloverhaul.client.render.RenderThirstGui;

@Mixin(value = RenderThirstGui.class, remap = false)
public class RenderThirstGuiMixin {

    @Inject(method = "drawHydrationBar", at = @At("HEAD"), cancellable = true)
    private static void raven_dnd_origins$hideThirstForUndead(GuiGraphics gui, Player player, int width, int height, int rightHeight, CallbackInfo ci) {
        if (player != null && ThirstImmunityPower.has(player)) {
            ci.cancel();
        }
    }
}
