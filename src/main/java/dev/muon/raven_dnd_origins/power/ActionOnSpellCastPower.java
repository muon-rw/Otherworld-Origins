package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Runs an entity action when an Iron's Spellbooks cast finishes server-side
 * ({@link AbstractSpell#onServerCastComplete} after vanilla cleanup).
 * <p>
 * {@link CastSpellConditions} defaults exclude {@link CastSource#COMMAND} so {@code cast_spell}
 * follow-ups do not re-trigger this power. Datapacks may set {@code cast_sources} to include
 * {@code command} when effects are chance-gated (e.g. Wild Magic surge) so recursion stays rare.
 */
public class ActionOnSpellCastPower extends PowerType<ActionOnSpellCastPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Configuration::entityAction),
            CastSpellConditions.CODEC.optionalFieldOf("cast_conditions", CastSpellConditions.defaults())
                    .forGetter(Configuration::castConditions)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
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
        PowerLookup.forEach(caster, ModPowers.ACTION_ON_SPELL_CAST, Configuration.class, config -> {
            if (config.castConditions().matches(spell, castSource, castType)) {
                config.entityAction().run(new EntityCtx(caster, caster.level()));
            }
        });
    }

    public record Configuration(EntityAction entityAction, CastSpellConditions castConditions) {
    }
}
