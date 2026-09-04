package dev.muon.raven_dnd_origins.util;

import dev.muon.raven_dnd_origins.power.ModifyDamageTakenDirectPower;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.DamageCtx;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Shared targeting rules for powers that react to incoming damage (holder = damaged entity).
 * Matches {@link ModifyDamageTakenDirectPower#matches}: damage condition, then attacker/direct entity for bientity checks.
 */
public final class DamageTakenPowerConditions {

    private DamageTakenPowerConditions() {}

    public static boolean matches(
            Optional<DamageCondition> damageCondition,
            Optional<BiEntityCondition> biEntityCondition,
            LivingEntity damaged,
            DamageSource source,
            float amount
    ) {
        Level level = damaged.level();
        if (damageCondition.isPresent() && !damageCondition.get().test(new DamageCtx(source, damaged, level, amount))) {
            return false;
        }
        Entity attacker = source.getDirectEntity();
        if (attacker == null) {
            attacker = source.getEntity();
        }
        if (attacker == null) {
            return biEntityCondition.isEmpty();
        }
        return biEntityCondition.isEmpty() || biEntityCondition.get().test(new BiEntityCtx(damaged, attacker, level));
    }
}
