package dev.muon.raven_dnd_origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.effect.ModEffects;
import dev.muon.raven_dnd_origins.power.ActionOnSpellDamagePower;
import dev.muon.raven_dnd_origins.power.EffectCategoryImmunityPower;
import dev.muon.raven_dnd_origins.power.MobsIgnorePower;
import dev.muon.raven_dnd_origins.power.ModifyStatusEffectCategoryPower;
import dev.muon.raven_dnd_origins.power.SuffocationImmunityPower;
import dev.muon.raven_dnd_origins.util.spell.OutgoingHealContext;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
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
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftEquipmentHandler;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * When an Iron's Spellbooks heal just posted {@code SpellHealEvent} for {@code this} as target, use heal amount
     * after {@code apoli:modify_healing} on the caster (see
     * {@link dev.muon.raven_dnd_origins.mixin.compat.irons_spellbooks.SpellHealEventMixin}).
     */
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float raven_dnd_origins$ironsSpellOutgoingHeal(float healAmount) {
        return OutgoingHealContext.consumeFor((LivingEntity) (Object) this, healAmount);
    }

    /**
     * {@link ActionOnSpellDamagePower}: after {@link LivingEntity#setHealth} runs for damage in {@code actuallyHurt}
     * ({@code f1 != 0} branch). {@link WrapOperation} chains with other mods; NeoForge's
     * {@code LivingDamageEvent.Pre} still fires before {@code setHealth} and {@code LivingDamageEvent.Post} after it.
     */
    @WrapOperation(
            method = "actuallyHurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V")
    )
    private void raven_dnd_origins$actionOnSpellDamageAfterSetHealth(
            LivingEntity instance, float health, Operation<Void> original, @Local(argsOnly = true) DamageSource damageSource
    ) {
        original.call(instance, health);
        if (damageSource instanceof SpellDamageSource sds) {
            ActionOnSpellDamagePower.afterSpellDamageApplied((LivingEntity) (Object) this, sds);
        }
    }

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$undeadWaterBreath(int currentAir, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player
                && SuffocationImmunityPower.has(player)
                && self.isEyeInFluid(FluidTags.WATER)) {
            cir.setReturnValue(currentAir);
        }
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$preventAttackValidation(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (target instanceof Player player && MobsIgnorePower.preventsMobFromTargeting(livingEntity, player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;)Z",
            at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$preventAttackValidationWithConditions(LivingEntity target, TargetingConditions condition, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (target instanceof Player player && MobsIgnorePower.preventsMobFromTargeting(livingEntity, player)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyReturnValue(method = "isCurrentlyGlowing", at = @At("RETURN"))
    private boolean raven_dnd_origins$favoredFoeGlowing(boolean original) {
        if (original) return true;
        LivingEntity self = (LivingEntity) (Object) this;
        return self.hasEffect(ModEffects.FAVORED_FOE) || self.hasEffect(ModEffects.DIVE_BOMB_MARK);
    }

    /**
     * The single-argument overload is final and only delegates here, and JustLevelingFork 1.2.9 now modifies the
     * effect on this overload too, so both immunity and duration scaling live on this one injection.
     */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$onAddEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (EffectCategoryImmunityPower.isImmune(self, effect)) {
            cir.setReturnValue(false);
            return;
        }
        raven_dnd_origins$modifyEffectDuration(self, effect);
    }

    /**
     * Wild shape keeps armor equipped but takes its numbers away: skip the modifier add for armor
     * slots while a suppressing form is active. The previous stack's removal (ordinal 0) still runs,
     * so swapping armor mid-form never leaves stale modifiers behind, and the enchantment
     * location effects vanilla fires from the same lambda are re-issued so they are not lost.
     */
    @WrapOperation(
            method = "collectEquipmentChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
                    ordinal = 1
            )
    )
    private void raven_dnd_origins$skipSuppressedArmorModifiers(ItemStack stack, EquipmentSlot slot,
                                                                BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
                                                                Operation<Void> original) {
        if (slot.isArmor() && (Object) this instanceof Player player
                && ShapeshiftEquipmentHandler.suppressesArmorModifiers(player)) {
            // Vanilla runs the location-changed enchantment effects inside the skipped lambda; keep those
            if (player.level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.runLocationChangedEffects(serverLevel, stack, player, slot);
            }
            return;
        }
        original.call(stack, slot, consumer);
    }

    @Unique
    private static void raven_dnd_origins$modifyEffectDuration(LivingEntity self, MobEffectInstance effect) {
        // ServerPlayer.restoreFrom copies effects onto a freshly built player through addEffect (End exit,
        // keep-inventory respawn). Those instances already carry a multiplied duration and the new player
        // has nothing to compare against, so every dimension change would compound them.
        if (!self.isAddedToLevel()) {
            return;
        }
        MobEffectInstance existing = self.getEffect(effect.getEffect());
        if (existing != null && existing.getDuration() >= effect.getDuration()) {
            return;
        }

        float multiplier = ModifyStatusEffectCategoryPower.getDurationMultiplier(self, effect);
        if (multiplier != 1.0f) {
            effect.duration = Math.round(effect.getDuration() * multiplier);
        }
    }

}
