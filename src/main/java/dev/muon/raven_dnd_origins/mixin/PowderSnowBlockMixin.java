package dev.muon.raven_dnd_origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.power.WalkOnPowderSnowPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {
    @ModifyReturnValue(
            method = "canEntityWalkOnPowderSnow(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("RETURN"))
    private static boolean raven_dnd_origins$allowPowderSnowWalk(boolean original, @Local(argsOnly = true) Entity entity) {
        if (!original) {
            return WalkOnPowderSnowPower.has(entity);
        }
        return true;
    }
}
