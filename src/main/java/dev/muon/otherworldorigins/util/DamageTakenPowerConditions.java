package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.power.ModifyDamageTakenDirectPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredDamageCondition;
import io.github.edwinmindcraft.apoli.common.registry.condition.ApoliDefaultConditions;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * Shared targeting rules for powers that react to incoming damage (holder = damaged entity).
 * Matches {@link ModifyDamageTakenDirectPower#check}: damage condition, then attacker/direct entity for bientity checks.
 */
public final class DamageTakenPowerConditions {

    private DamageTakenPowerConditions() {}

    public static boolean matches(
            Holder<ConfiguredDamageCondition<?, ?>> damageCondition,
            Holder<ConfiguredBiEntityCondition<?, ?>> biEntityCondition,
            Entity damaged,
            DamageSource source,
            float amount
    ) {
        if (!ConfiguredDamageCondition.check(damageCondition, source, amount)) {
            return false;
        }
        Entity damageSource = source.getDirectEntity();
        if (damageSource == null || damageSource == source.getEntity()) {
            damageSource = source.getEntity();
        }
        if (damageSource == null) {
            return biEntityCondition.is(ApoliDefaultConditions.BIENTITY_DEFAULT.getId());
        }
        return ConfiguredBiEntityCondition.check(biEntityCondition, damaged, damageSource);
    }
}
