package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.ModifyStatusEffectCategoryPower;
import io.github.apace100.apoli.component.PowerHolderComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", at = @At("HEAD"))
    private void onAddEffectNoEntity(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        otherworld$modifyEffectDuration(effect, null);
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void onAddEffectWithEntity(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        otherworld$modifyEffectDuration(effect, source);
    }

    @Unique
    private void otherworld$modifyEffectDuration(MobEffectInstance effect, Entity source) {
        LivingEntity self = (LivingEntity) (Object) this;

        float multiplier = otherworld$getEffectMultiplier(self, effect);
        if (multiplier != 1.0f) {
            int newDuration = Math.round(effect.getDuration() * multiplier);
            ((MobEffectInstanceAccessor) effect).setDuration(newDuration);
        }
    }

    @Unique
    private static float otherworld$getEffectMultiplier(LivingEntity entity, MobEffectInstance effect) {
        return (float) PowerHolderComponent.getPowers(entity, ModifyStatusEffectCategoryPower.class).stream()
                .filter(ModifyStatusEffectCategoryPower::isActive)
                .filter(powerType -> powerType.doesApply(effect.getEffect()))
                .mapToDouble(ModifyStatusEffectCategoryPower::getAmount)
                .reduce(1.0f, (a, b) -> (float) (a * b));
    }
}
