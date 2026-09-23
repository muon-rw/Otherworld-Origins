package dev.muon.raven_dnd_origins.mixin.compat.caelus;

import com.illusivesoulworks.caelus.api.CaelusApi;
import com.illusivesoulworks.caelus.common.CaelusApiImpl;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.power.builtin.PreventElytraFlightPower;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Caelus starts flight directly on ALLOW, bypassing the {@code tryToStartFallFlying} hook that
 * {@code apoli:prevent_elytra_flight} relies on.
 */
@Mixin(value = CaelusApiImpl.class, remap = false)
public class CaelusApiImplMixin {
    @ModifyReturnValue(method = "canFallFly(Lnet/minecraft/world/entity/LivingEntity;)Lcom/illusivesoulworks/caelus/api/CaelusApi$TriState;", at = @At("RETURN"))
    private CaelusApi.TriState raven_dnd_origins$denyWhenElytraFlightPrevented(CaelusApi.TriState original, LivingEntity livingEntity) {
        return PreventElytraFlightPower.isPrevented(livingEntity) ? CaelusApi.TriState.DENY : original;
    }
}
