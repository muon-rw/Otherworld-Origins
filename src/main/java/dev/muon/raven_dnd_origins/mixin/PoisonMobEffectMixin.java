package dev.muon.raven_dnd_origins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.raven_dnd_origins.damage.ModDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces the poison tick damage source (NeoForge already swaps vanilla {@code minecraft:magic} for
 * {@code neoforge:poison} here) with {@code raven_dnd_origins:poison}, which the pack's
 * {@code forge:is_poison}/{@code c:is_poison}/{@code raven_dnd_origins:is_magic} damage type tags list
 * (e.g. for the Land Druid's {@code healing_from_poison} power).
 */
@Mixin(targets = "net.minecraft.world.effect.PoisonMobEffect")
public abstract class PoisonMobEffectMixin {

    @WrapOperation(
            method = "applyEffectTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private boolean raven_dnd_origins$tagPoisonEffectDamage(LivingEntity target, DamageSource source, float amount, Operation<Boolean> original) {
        DamageSource tagged = target.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(ModDamageTypes.POISON)
                .map(DamageSource::new)
                .orElse(source);
        return original.call(target, tagged, amount);
    }
}
