package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.compat.irons_spellbooks.IronsSpellOutgoingHealContext;
import dev.muon.otherworldorigins.effect.ModEffects;
import dev.muon.otherworldorigins.power.*;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * When an Iron's Spellbooks heal just posted {@code SpellHealEvent} for {@code this} as target, use heal amount
     * after {@code apoli:modify_healing} on the caster (see
     * {@link dev.muon.otherworldorigins.mixin.compat.irons_spellbooks.SpellHealEventMixin}).
     */
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float otherworldorigins$ironsSpellOutgoingHeal(float healAmount) {
        return IronsSpellOutgoingHealContext.consumeFor((LivingEntity) (Object) this, healAmount);
    }

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$undeadWaterBreath(int currentAir, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player
                && SuffocationImmunityPower.has(player)
                && self.isEyeInFluid(FluidTags.WATER)) {
            cir.setReturnValue(currentAir);
        }
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void otherworld$preventAttackValidation(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (target instanceof Player player && MobsIgnorePower.preventsMobFromTargeting(livingEntity, player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;)Z",
            at = @At("HEAD"), cancellable = true)
    private void otherworld$preventAttackValidationWithConditions(LivingEntity target, TargetingConditions condition, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (target instanceof Player player && MobsIgnorePower.preventsMobFromTargeting(livingEntity, player)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyReturnValue(method = "isCurrentlyGlowing", at = @At("RETURN"))
    private boolean otherworld$favoredFoeGlowing(boolean original) {
        if (original) return true;
        LivingEntity self = (LivingEntity) (Object) this;
        return self.hasEffect(ModEffects.FAVORED_FOE.get()) || self.hasEffect(ModEffects.DIVE_BOMB_MARK.get());
    }

    /**
     * JustLevelingFork intercepts the 1-arg addEffect for Players, cancelling
     * delegation to the 2-arg overload and handling effects via its own logic.
     * It reads effect.getDuration() to build the final MobEffectInstance, so we
     * must modify duration here before JLF's injection point.
     * Only targets Players to avoid double-application on non-Players (where the
     * 1-arg still delegates to the 2-arg normally).
     */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", at = @At("HEAD"))
    private void otherworld$onAddEffectSingleArg(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player)) return;
        otherworld$modifyEffectDuration(self, effect);
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void otherworld$onAddEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        otherworld$modifyEffectDuration(self, effect);
    }

    @Unique
    private static void otherworld$modifyEffectDuration(LivingEntity self, MobEffectInstance effect) {
        MobEffectInstance existing = self.getEffect(effect.getEffect());
        if (existing != null && existing.getDuration() >= effect.getDuration()) {
            return;
        }

        float multiplier = otherworld$getEffectMultiplier(self, effect);
        if (multiplier != 1.0f) {
            int newDuration = Math.round(effect.getDuration() * multiplier);
            ((MobEffectInstanceAccessor) effect).setDuration(newDuration);
        }
    }

    @Unique
    private static float otherworld$getEffectMultiplier(LivingEntity entity, MobEffectInstance effect) {
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(entity);
        if (powerContainer != null) {
            return powerContainer.getPowers(ModPowers.MODIFY_STATUS_EFFECT_CATEGORY.get()).stream()
                    .filter(holder -> ModifyStatusEffectCategoryPower.doesApply(holder.value().getConfiguration(), effect.getEffect()))
                    .map(holder -> holder.value().getConfiguration().amount())
                    .reduce(1.0f, (a, b) -> a * b);
        }
        return 1.0f;
    }

}