package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks;

import dev.muon.otherworldorigins.compat.irons_spellbooks.IronsSpellOutgoingHealContext;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.common.power.ModifyValuePower;
import io.github.edwinmindcraft.apoli.common.registry.ApoliPowers;
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
    private void otherworldorigins$applyOutgoingModifyHealing(
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
        float modified = IPowerContainer.modify(
                castingEntity,
                ApoliPowers.MODIFY_HEALING.get(),
                healAmount
        );
        IronsSpellOutgoingHealContext.pushOutgoingHeal(targetEntity, modified);
    }
}
