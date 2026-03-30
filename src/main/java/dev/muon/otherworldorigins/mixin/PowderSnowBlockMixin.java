package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.power.WalkOnPowderSnowPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {
    @ModifyReturnValue(
            method = "canEntityWalkOnPowderSnow(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("RETURN"))
    private static boolean canEntityWalkOnPowderSnow(boolean original, @Local(argsOnly = true) Entity entity) {
        if (!original) {
            return WalkOnPowderSnowPower.has(entity);
        }
        return true;
    }
}
