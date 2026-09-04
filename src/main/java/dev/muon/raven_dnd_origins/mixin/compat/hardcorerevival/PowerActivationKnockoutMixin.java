package dev.muon.raven_dnd_origins.mixin.compat.hardcorerevival;

import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.network.payload.PowerToggleC2S;
import net.blay09.mods.hardcorerevival.api.HardcoreRevivalAPI;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Apoli routes keybound power use through {@code Apoli#handleActivation} and {@code Apoli#handleToggle};
 * a knocked-out player must not be able to fire or toggle one.
 */
@Mixin(value = Apoli.class, remap = false)
public class PowerActivationKnockoutMixin {

    @Inject(method = "handleActivation", at = @At("HEAD"), cancellable = true)
    private static void raven_dnd_origins$blockActivePowerWhileKnockedOut(
            ServerPlayer player, PowerActivationC2S payload, CallbackInfo ci) {
        if (HardcoreRevivalAPI.isKnockedOut(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleToggle", at = @At("HEAD"), cancellable = true)
    private static void raven_dnd_origins$blockPowerToggleWhileKnockedOut(
            ServerPlayer player, PowerToggleC2S payload, CallbackInfo ci) {
        if (HardcoreRevivalAPI.isKnockedOut(player)) {
            ci.cancel();
        }
    }
}
