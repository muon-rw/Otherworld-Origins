package dev.muon.otherworldorigins.mixin.client.compat.legendarysurvivaloverhaul;

import dev.muon.otherworldorigins.power.UndeadVitalsPower;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sfiomn.legendarysurvivaloverhaul.client.render.RenderThirstGui;

@Mixin(value = RenderThirstGui.class, remap = false)
public class RenderThirstGuiMixin {

    @Inject(method = "drawHydrationBar", at = @At("HEAD"), cancellable = true)
    private static void otherworldorigins$hideThirstForUndead(ForgeGui gui, GuiGraphics graphics, Player player, int x, int y, CallbackInfo ci) {
        if (player != null && UndeadVitalsPower.has(player)) {
            ci.cancel();
        }
    }
}
