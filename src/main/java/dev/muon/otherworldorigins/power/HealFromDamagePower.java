package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.DamageTakenPowerConditions;
import io.github.apace100.apoli.util.modifier.ModifierUtil;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.ListConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredDamageCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredModifier;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * When incoming damage matches {@code damage_condition} (and optional {@code bientity_condition}),
 * that damage is negated and the entity is healed by the hit amount after optional {@code modifier}(s)
 * / {@code modifiers} (same shape as {@code origins:modify_damage_taken}).
 * Multiple active matching powers on one entity: only the first match applies per hit.
 */
public class HealFromDamagePower extends PowerFactory<HealFromDamagePower.Configuration> {

    public HealFromDamagePower() {
        super(Configuration.CODEC);
    }

    /**
     * @return remaining damage after converting matching damage into healing (typically 0 when matched)
     */
    public static float apply(Entity entity, DamageSource source, float amount) {
        if (!(entity instanceof LivingEntity living) || entity.level().isClientSide() || amount <= 0f) {
            return amount;
        }
        return IPowerContainer.get(entity)
                .map(container -> apply(container, living, source, amount))
                .orElse(amount);
    }

    private static float apply(IPowerContainer container, LivingEntity living, DamageSource source, float amount) {
        float incoming = amount;
        for (Holder<ConfiguredPower<Configuration, HealFromDamagePower>> holder : container.getPowers(ModPowers.HEAL_FROM_DAMAGE.get())) {
            if (!holder.isBound()) {
                continue;
            }
            ConfiguredPower<Configuration, HealFromDamagePower> power = holder.value();
            if (!power.isActive(living)) {
                continue;
            }
            Configuration cfg = power.getConfiguration();
            if (!DamageTakenPowerConditions.matches(cfg.damageCondition(), cfg.biEntityCondition(), living, source, incoming)) {
                continue;
            }
            double heal = ModifierUtil.applyModifiers(living, cfg.healModifier().getContent(), incoming);
            if (heal > 0d) {
                living.heal((float) heal);
            }
            return 0f;
        }
        return incoming;
    }

    public record Configuration(
            ListConfiguration<ConfiguredModifier<?>> healModifier,
            Holder<ConfiguredDamageCondition<?, ?>> damageCondition,
            Holder<ConfiguredBiEntityCondition<?, ?>> biEntityCondition
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ListConfiguration.MODIFIER_CODEC.forGetter(Configuration::healModifier),
                ConfiguredDamageCondition.optional("damage_condition").forGetter(Configuration::damageCondition),
                ConfiguredBiEntityCondition.optional("bientity_condition").forGetter(Configuration::biEntityCondition)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
