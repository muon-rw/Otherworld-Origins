package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks;

import dev.muon.otherworldorigins.power.SpellImmunityPower;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Predicate;

@Mixin(value = Utils.class, remap = false)
public class UtilsMixin {

    @ModifyVariable(
            method = "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Z",
            at = @At("HEAD"),
            argsOnly = true,
            index = 7
    )
    private static Predicate<LivingEntity> otherworld$filterSpellImmuneTargets(
            Predicate<LivingEntity> filter,
            Level level,
            LivingEntity caster,
            MagicData playerMagicData,
            AbstractSpell spell,
            int range,
            float aimAssist,
            boolean sendFailureMessage
    ) {
        if (spell == null) {
            return filter;
        }
        return filter.and(target -> !SpellImmunityPower.hasSpellImmunity(target, spell.getSpellResource()));
    }
}
