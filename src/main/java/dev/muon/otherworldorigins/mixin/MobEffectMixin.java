package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.damage.ModDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Replaces the vanilla {@code minecraft:magic} damage source used by the {@link net.minecraft.world.effect.MobEffects#POISON}
 * tick with a dedicated {@code otherworldorigins:poison} damage source so it can be matched by
 * {@code forge:is_poison}/{@code c:is_poison}/{@code otherworldorigins:is_magic} damage type tags
 * (e.g. by the Land Druid's {@code healing_from_poison} power). The expression matches on the
 * literal {@code 1.0F} damage amount so the later HARM/HEAL branch — which calls {@code magic()}
 * with a computed {@code (float)(6 << amplifier)} — is left untouched.
 */
@Mixin(MobEffect.class)
public abstract class MobEffectMixin {

    @Definition(id = "magic", method = "Lnet/minecraft/world/damagesource/DamageSources;magic()Lnet/minecraft/world/damagesource/DamageSource;")
    @Definition(id = "hurt", method = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    @Expression("?.hurt(@(?.magic()), 1.0)")
    @ModifyExpressionValue(method = "applyEffectTick", at = @At("MIXINEXTRAS:EXPRESSION"))
    private DamageSource otherworldorigins$tagPoisonEffectDamage(DamageSource original, @Local(argsOnly = true) LivingEntity livingEntity) {
        return livingEntity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(ModDamageTypes.POISON)
                .<DamageSource>map(DamageSource::new)
                .orElse(original);
    }
}
