package dev.muon.otherworldorigins.action.bientity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.util.SpellSelection;
import dev.muon.otherworldorigins.util.SpellCastUtil;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * A bi-entity action that casts a spell from the actor towards the target.
 * Unlike {@link dev.muon.otherworldorigins.action.entity.CastSpellAction}, this does not use
 * {@link SpellCastUtil#findTarget} — the target is provided directly. Player casts share
 * {@link SpellCastUtil#castSpellForPlayerWithBientityTarget}.
 */
public class CastSpellBientityAction extends BiEntityAction<CastSpellBientityAction.Configuration> {

    public CastSpellBientityAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(actor instanceof LivingEntity caster)) {
            OtherworldOrigins.LOGGER.debug("CastSpellBientityAction: Actor is not a LivingEntity: {}", actor);
            return;
        }

        if (!(target instanceof LivingEntity livingTarget)) {
            OtherworldOrigins.LOGGER.debug("CastSpellBientityAction: Target is not a LivingEntity: {}", target);
            return;
        }

        Level world = actor.level();
        if (world.isClientSide) {
            return;
        }

        SpellSelection.ResolvedSpell resolved = configuration.spell().resolve(world.random);
        if (resolved == null) {
            return;
        }
        AbstractSpell spell = resolved.spell();
        int powerLevel = resolved.level();
        MagicData magicData = MagicData.getPlayerMagicData(caster);

        if (magicData.isCasting()) {
            SpellCastUtil.forceCompleteCurrentCastIfAny(caster, world);
            magicData = MagicData.getPlayerMagicData(caster);
        }

        if (caster instanceof ServerPlayer serverPlayer) {
            SpellCastUtil.castSpellForPlayerWithBientityTarget(
                    spell,
                    powerLevel,
                    serverPlayer,
                    magicData,
                    world,
                    livingTarget,
                    configuration.castTime(),
                    configuration.manaCost(),
                    configuration.continuousCost(),
                    configuration.costInterval()
            );
        } else if (caster instanceof IMagicEntity magicEntity) {
            magicEntity.initiateCastSpell(spell, powerLevel);
        } else {
            if (spell.checkPreCastConditions(world, powerLevel, caster, magicData)) {
                SpellCastUtil.maybeApplyBientityProvidedTarget(caster, livingTarget, magicData, spell);
                spell.onCast(world, powerLevel, caster, CastSource.COMMAND, magicData);
                spell.onServerCastComplete(world, powerLevel, caster, magicData, false);
            }
        }
    }

    public static class Configuration implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.either(ResourceLocation.CODEC, SpellSelection.OBJECT_CODEC)
                        .fieldOf("spell")
                        .forGetter(Configuration::spellFieldForCodec),
                Codec.INT.optionalFieldOf("power_level").forGetter(Configuration::legacyPowerLevelForCodec),
                Codec.INT.optionalFieldOf("cast_time").forGetter(Configuration::castTime),
                Codec.INT.optionalFieldOf("mana_cost").forGetter(Configuration::manaCost),
                Codec.BOOL.optionalFieldOf("continuous_cost", false).forGetter(Configuration::continuousCost),
                Codec.INT.optionalFieldOf("cost_interval", 20).forGetter(Configuration::costInterval)
        ).apply(instance, Configuration::create));

        private final SpellSelection spell;
        private final Optional<Integer> castTime;
        private final Optional<Integer> manaCost;
        private final boolean continuousCost;
        private final int costInterval;

        private static Configuration create(
                Either<ResourceLocation, SpellSelection> spellField,
                Optional<Integer> legacyPowerLevel,
                Optional<Integer> castTime,
                Optional<Integer> manaCost,
                boolean continuousCost,
                int costInterval
        ) {
            SpellSelection selection = spellField.map(
                    rl -> SpellSelection.fromLegacyStringForm(rl, legacyPowerLevel.orElse(1)),
                    s -> s
            );
            return new Configuration(selection, castTime, manaCost, continuousCost, costInterval);
        }

        public Configuration(SpellSelection spell, Optional<Integer> castTime,
                             Optional<Integer> manaCost, boolean continuousCost, int costInterval) {
            this.spell = spell;
            this.castTime = castTime;
            this.manaCost = manaCost;
            this.continuousCost = continuousCost;
            this.costInterval = costInterval;
        }

        private Either<ResourceLocation, SpellSelection> spellFieldForCodec() {
            return spell.encodeForActionSpellField();
        }

        private Optional<Integer> legacyPowerLevelForCodec() {
            return spell.legacyPowerLevelForCodec();
        }

        public SpellSelection spell() {
            return spell;
        }

        public Optional<Integer> castTime() {
            return castTime;
        }

        public Optional<Integer> manaCost() {
            return manaCost;
        }

        public boolean continuousCost() {
            return continuousCost;
        }

        public int costInterval() {
            return costInterval;
        }
    }
}
