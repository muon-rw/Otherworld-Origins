package dev.muon.raven_dnd_origins.mixin.compat.icarus;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.cammiescorner.icarus.client.IcarusClient;
import dev.muon.raven_dnd_origins.power.BypassArmorWeightPower;
import dev.muon.raven_dnd_origins.power.PreventWingBoostPower;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IcarusClient.class, remap = false)
public class IcarusClientMixin {
    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true)
    private static void raven_dnd_origins$preventWingBoost(AbstractClientPlayer player, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (PreventWingBoostPower.has(player)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "onPlayerTick", at = @At(value = "INVOKE", target = "Ldev/cammiescorner/icarus/api/IcarusPlayerValues;armorSlows()Z"))
    private static boolean raven_dnd_origins$bypassArmorWeight(boolean original, AbstractClientPlayer player, Level level) {
        return original && !BypassArmorWeightPower.has(player);
    }
}
