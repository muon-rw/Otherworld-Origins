package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.effect.ModEffects;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyStatusEffectCategoryPower;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
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

    @ModifyReturnValue(method = "isCurrentlyGlowing", at = @At("RETURN"))
    private boolean otherworld$favoredFoeGlowing(boolean original) {
        if (original) return true;
        LivingEntity self = (LivingEntity) (Object) this;
        return self.hasEffect(ModEffects.FAVORED_FOE.get());
    }

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