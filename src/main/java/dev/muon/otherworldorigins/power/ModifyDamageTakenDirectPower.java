package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.ListConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredDamageCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredModifier;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.power.IValueModifyingPowerConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.power.ValueModifyingPowerFactory;
import io.github.edwinmindcraft.apoli.common.registry.condition.ApoliDefaultConditions;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public class ModifyDamageTakenDirectPower extends ValueModifyingPowerFactory<ModifyDamageTakenDirectPower.Configuration> {

    public ModifyDamageTakenDirectPower() {
        super(Configuration.CODEC);
    }

    public static float modify(Entity entity, DamageSource source, float amount) {
        return IPowerContainer.modify(entity, ModPowers.MODIFY_DAMAGE_TAKEN.get(), amount,
                holder -> ((ModifyDamageTakenDirectPower) holder.value().getFactory()).check(holder.value(), entity, source, amount),
                holder -> ((ModifyDamageTakenDirectPower) holder.value().getFactory()).execute(holder.value(), entity, source));
    }

    /**
     * BiEntity convention: actor = power holder (entity taking damage), target = damage source (entity causing damage).
     * For projectile damage: (power holder, projectile). For melee: (power holder, attacker).
     */
    public boolean check(ConfiguredPower<Configuration, ?> config, Entity entity, DamageSource source, float amount) {
        Configuration configuration = config.getConfiguration();
        boolean damage = ConfiguredDamageCondition.check(configuration.damageCondition(), source, amount);
        if (!damage) return false;
        Entity damageSource = source.getDirectEntity();
        if (damageSource == null || damageSource == source.getEntity()) {
            damageSource = source.getEntity();
        }
        if (damageSource == null) {
            return configuration.biEntityCondition().is(ApoliDefaultConditions.BIENTITY_DEFAULT.getId());
        }
        return ConfiguredBiEntityCondition.check(configuration.biEntityCondition(), entity, damageSource);
    }

    public void execute(ConfiguredPower<Configuration, ?> config, Entity entity, DamageSource source) {
        Configuration configuration = config.getConfiguration();
        ConfiguredEntityAction.execute(configuration.selfAction(), entity);
        Entity attacker = source.getEntity();
        Entity directEntity = source.getDirectEntity();
        if (attacker != null) {
            ConfiguredEntityAction.execute(configuration.targetAction(), attacker);
            ConfiguredBiEntityAction.execute(configuration.biEntityAction(), entity, attacker);
        }
    }

    public boolean modifiesArmorApplicance(ConfiguredPower<Configuration, ?> config) {
        return !config.getConfiguration().applyArmorCondition().is(ApoliDefaultConditions.ENTITY_DEFAULT.getId());
    }

    public boolean checkArmorApplicance(ConfiguredPower<Configuration, ?> config, Entity entity) {
        return modifiesArmorApplicance(config) && ConfiguredEntityCondition.check(config.getConfiguration().applyArmorCondition(), entity);
    }

    public boolean modifiesArmorDamaging(ConfiguredPower<Configuration, ?> config) {
        return !config.getConfiguration().damageArmorCondition().is(ApoliDefaultConditions.ENTITY_DEFAULT.getId());
    }

    public boolean checkArmorDamaging(ConfiguredPower<Configuration, ?> config, Entity entity) {
        return modifiesArmorDamaging(config) && ConfiguredEntityCondition.check(config.getConfiguration().damageArmorCondition(), entity);
    }

    public record Configuration(
            ListConfiguration<ConfiguredModifier<?>> modifiers,
            Holder<ConfiguredDamageCondition<?, ?>> damageCondition,
            Holder<ConfiguredBiEntityCondition<?, ?>> biEntityCondition,
            Holder<ConfiguredEntityAction<?, ?>> selfAction,
            Holder<ConfiguredEntityAction<?, ?>> targetAction,
            Holder<ConfiguredBiEntityAction<?, ?>> biEntityAction,
            Holder<ConfiguredEntityCondition<?, ?>> applyArmorCondition,
            Holder<ConfiguredEntityCondition<?, ?>> damageArmorCondition
    ) implements IValueModifyingPowerConfiguration {

        @Override
        public ListConfiguration<ConfiguredModifier<?>> modifiers() {
            return modifiers;
        }

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ListConfiguration.MODIFIER_CODEC.forGetter(Configuration::modifiers),
                ConfiguredDamageCondition.optional("damage_condition").forGetter(Configuration::damageCondition),
                ConfiguredBiEntityCondition.optional("bientity_condition").forGetter(Configuration::biEntityCondition),
                ConfiguredEntityAction.optional("self_action").forGetter(Configuration::selfAction),
                ConfiguredEntityAction.optional("attacker_action").forGetter(Configuration::targetAction),
                ConfiguredBiEntityAction.optional("bientity_action").forGetter(Configuration::biEntityAction),
                ConfiguredEntityCondition.optional("apply_armor_condition").forGetter(Configuration::applyArmorCondition),
                ConfiguredEntityCondition.optional("damage_armor_condition").forGetter(Configuration::damageArmorCondition)
        ).apply(instance, Configuration::new));
    }
}
