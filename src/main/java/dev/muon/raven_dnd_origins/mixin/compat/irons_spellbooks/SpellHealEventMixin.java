package dev.muon.raven_dnd_origins.mixin.compat.irons_spellbooks;

import dev.muon.raven_dnd_origins.util.spell.OutgoingHealContext;
import dev.overgrown.apoli.power.builtin.ModifyHealingHandler;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpellHealEvent.class, remap = false)
public class SpellHealEventMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void raven_dnd_origins$applyOutgoingModifyHealing(
            LivingEntity castingEntity,
            LivingEntity targetEntity,
            float healAmount,
            SchoolType schoolType,
            CallbackInfo ci
    ) {
        if (castingEntity.level().isClientSide) {
            return;
        }
        if (castingEntity == targetEntity) {
            return;
        }
        OutgoingHealContext.pushOutgoingHeal(targetEntity, ModifyHealingHandler.modify(castingEntity, healAmount));
    }
}
