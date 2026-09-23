package dev.muon.raven_dnd_origins.mixin.compat.icarus;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.cammiescorner.icarus.util.IcarusHelper;
import dev.muon.raven_dnd_origins.power.PreventWingBoostPower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = IcarusHelper.class, remap = false)
public class IcarusHelperMixin {
    @WrapWithCondition(method = "onFallFlyingTick", at = @At(value = "INVOKE", target = "Ldev/cammiescorner/icarus/network/c2s/ApplyBoostPacket;sendToServer()V"))
    private static boolean raven_dnd_origins$skipBoostCostWhenBoostPrevented(LivingEntity entity, ItemStack wings, boolean tick) {
        return !PreventWingBoostPower.has(entity);
    }
}
