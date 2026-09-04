package dev.muon.raven_dnd_origins.mixin.compat.legendarysurvivaloverhaul;

import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.muon.raven_dnd_origins.power.ModifyThirstExhaustionPower;
import dev.muon.raven_dnd_origins.power.ThirstImmunityPower;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sfiomn.legendarysurvivaloverhaul.util.internal.ThirstUtilInternal;

@Mixin(value = ThirstUtilInternal.class, remap = false)
public class ThirstUtilInternalMixin {

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$undeadIgnoreThirstExhaustion(Player player, float exhaustion, CallbackInfo ci) {
        if (ThirstImmunityPower.has(player)) {
            ci.cancel();
        }
    }

    @ModifyArg(method = "addExhaustion",
            at = @At(value = "INVOKE", target = "Lsfiomn/legendarysurvivaloverhaul/common/attachments/thirst/ThirstAttachment;addThirstExhaustion(F)V"))
    private float raven_dnd_origins$scaleThirstExhaustion(float exhaustion, @Local(argsOnly = true) Player player) {
        float totalModifier = 0f;
        for (ModifyThirstExhaustionPower.Configuration cfg : PowerLookup.active(
                player, ModPowers.MODIFY_THIRST_EXHAUSTION, ModifyThirstExhaustionPower.Configuration.class)) {
            totalModifier += cfg.amount();
        }
        return totalModifier != 0 ? exhaustion * totalModifier : exhaustion;
    }
}
