package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.power.MobsIgnorePower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.entity.ai.targeting.TargetingConditions.class)
public class TargetingConditionsMixin {

    @ModifyReturnValue(method = "test(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"))
    private boolean otherworld$preventMobTargeting(boolean original, LivingEntity attacker, LivingEntity target) {
        if (original && attacker != null && target instanceof Player player) {
            if (MobsIgnorePower.preventsMobFromTargeting(attacker, player)) {
                return false;
            }
        }
        return original;
    }
}
