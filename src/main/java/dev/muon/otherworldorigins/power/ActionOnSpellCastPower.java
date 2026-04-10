package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;

/**
 * Runs an entity action when an Iron's Spellbooks cast finishes server-side
 * ({@link AbstractSpell#onServerCastComplete} after vanilla cleanup).
 * <p>
 * {@link CastSpellConditions} defaults exclude {@link CastSource#COMMAND} so {@code cast_spell}
 * follow-ups do not re-trigger this power. Datapacks may set {@code cast_sources} to include
 * {@code command} when effects are chance-gated (e.g. Wild Magic surge) so recursion stays rare.
 */
public class ActionOnSpellCastPower extends PowerFactory<ActionOnSpellCastPower.Configuration> {

    public ActionOnSpellCastPower() {
        super(Configuration.CODEC);
    }

    public static void handleSpellCastComplete(
            AbstractSpell spell,
            LivingEntity caster,
            int spellLevel,
            CastSource castSource,
            CastType castType
    ) {
        if (caster.level().isClientSide()) {
            return;
        }
        IPowerContainer.getPowers(caster, ModPowers.ACTION_ON_SPELL_CAST.get()).forEach(holder ->
                triggerIfMatch(holder.value(), spell, caster, spellLevel, castSource, castType)
        );
    }

    private static void triggerIfMatch(
            ConfiguredPower<Configuration, ?> power,
            AbstractSpell spell,
            LivingEntity caster,
            int spellLevel,
            CastSource castSource,
            CastType castType
    ) {
        Configuration config = power.getConfiguration();
        if (!config.castConditions().matches(spell, castSource, castType)) {
            return;
        }
        ConfiguredEntityAction.execute(config.entityAction(), caster);
    }

    public record Configuration(
            Holder<ConfiguredEntityAction<?, ?>> entityAction,
            CastSpellConditions castConditions
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ConfiguredEntityAction.required("entity_action").forGetter(Configuration::entityAction),
                CastSpellConditions.CODEC.optionalFieldOf("cast_conditions", CastSpellConditions.defaults())
                        .forGetter(Configuration::castConditions)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
