package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.DamageTakenPowerConditions;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

/**
 * When incoming damage matches {@code damage_condition} (and optional {@code bientity_condition}),
 * the attack is cancelled outright and the entity is healed by the hit amount after optional
 * {@code modifier}(s) / {@code modifiers} (same shape as {@code apoli:modify_damage_taken}).
 * Hooked at {@code LivingIncomingDamageEvent} so cancellation also suppresses the hurt sound, red flash,
 * and damage-tilt camera shake, none of which can be cancelled from {@code LivingDamageEvent}.
 * Multiple active matching powers on one entity: only the first match applies per hit.
 */
public class HealFromDamagePower extends PowerType<HealFromDamagePower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Configuration::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Configuration::modifiers),
            LoggedOptionalField.strict("damage_condition", DamageCondition.CODEC).forGetter(Configuration::damageCondition),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Configuration::biEntityCondition)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    /**
     * @return {@code true} if a power matched and the caller should cancel the attack
     */
    public static boolean apply(Entity entity, DamageSource source, float amount) {
        if (!(entity instanceof LivingEntity living) || entity.level().isClientSide() || amount <= 0f) {
            return false;
        }
        for (Configuration cfg : PowerLookup.active(living, ModPowers.HEAL_FROM_DAMAGE, Configuration.class)) {
            if (!DamageTakenPowerConditions.matches(cfg.damageCondition(), cfg.biEntityCondition(), living, source, amount)) {
                continue;
            }
            float heal = AttributeModifierHelper.apply(amount, cfg.allModifiers(), living);
            if (heal > 0f) {
                living.heal(heal);
            }
            return true;
        }
        return false;
    }

    public record Configuration(
            Optional<AttributeModifier> modifier,
            Optional<List<AttributeModifier>> modifiers,
            Optional<DamageCondition> damageCondition,
            Optional<BiEntityCondition> biEntityCondition
    ) {
        public List<AttributeModifier> allModifiers() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }
}
