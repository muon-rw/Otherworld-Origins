package dev.muon.otherworldorigins.action.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.util.spell.SpellCastInterruptMode;
import dev.muon.otherworldorigins.util.spell.SpellSelection;
import dev.muon.otherworldorigins.util.spell.SpellCastUtil;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Apoli entity action: resolve a {@link SpellSelection} and cast via {@link SpellCastUtil}.
 */
public class CastSpellAction extends EntityAction<CastSpellAction.Configuration> {

    public CastSpellAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            OtherworldOrigins.LOGGER.debug("Entity is not a LivingEntity: {}", entity);
            return;
        }

        Level world = entity.level();
        if (world.isClientSide) {
            return;
        }

        SpellSelection.ResolvedSpell resolved = configuration.spell().resolve(world.random);
        if (resolved == null) {
            return;
        }
        AbstractSpell spell = resolved.spell();
        int powerLevel = resolved.level();
        MagicData magicData = MagicData.getPlayerMagicData(livingEntity);
        magicData = SpellCastUtil.resolveBusyCastBeforeNewSpell(livingEntity, world, magicData, configuration.interruptMode(), spell);
        if (magicData == null) {
            return;
        }

        LivingEntity raycastTarget = SpellCastUtil.findTarget(livingEntity, configuration.raycastDistance().orElse(SpellCastUtil.DEFAULT_RAYCAST_DISTANCE));

        if (livingEntity instanceof ServerPlayer serverPlayer) {
            SpellCastUtil.castSpellForPlayer(
                    spell,
                    powerLevel,
                    serverPlayer,
                    magicData,
                    world,
                    raycastTarget,
                    configuration.castTime(),
                    configuration.manaCost(),
                    configuration.continuousCost(),
                    configuration.costInterval()
            );
        } else if (livingEntity instanceof IMagicEntity magicEntity) {
            magicEntity.initiateCastSpell(spell, powerLevel);
        } else {
            if (spell.checkPreCastConditions(world, powerLevel, livingEntity, magicData)) {
                SpellCastUtil.maybeUpdateTargetData(livingEntity, raycastTarget, magicData, spell);
                spell.onCast(world, powerLevel, livingEntity, CastSource.COMMAND, magicData);
                spell.onServerCastComplete(world, powerLevel, livingEntity, magicData, false);
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
                Codec.INT.optionalFieldOf("cost_interval", 20).forGetter(Configuration::costInterval),
                Codec.DOUBLE.optionalFieldOf("raycast_distance").forGetter(Configuration::raycastDistance),
                SpellCastInterruptMode.CODEC.optionalFieldOf("interrupt_mode", SpellCastInterruptMode.CANCEL).forGetter(Configuration::interruptMode)
        ).apply(instance, Configuration::create));

        private final SpellSelection spell;
        private final Optional<Integer> castTime;
        private final Optional<Integer> manaCost;
        private final boolean continuousCost;
        private final int costInterval;
        private final Optional<Double> raycastDistance;
        private final SpellCastInterruptMode interruptMode;

        private static Configuration create(
                Either<ResourceLocation, SpellSelection> spellField,
                Optional<Integer> legacyPowerLevel,
                Optional<Integer> castTime,
                Optional<Integer> manaCost,
                boolean continuousCost,
                int costInterval,
                Optional<Double> raycastDistance,
                SpellCastInterruptMode interruptMode
        ) {
            SpellSelection selection = spellField.map(
                    rl -> SpellSelection.fromLegacyStringForm(rl, legacyPowerLevel.orElse(1)),
                    s -> s
            );
            return new Configuration(selection, castTime, manaCost, continuousCost, costInterval, raycastDistance, interruptMode);
        }

        public Configuration(SpellSelection spell, Optional<Integer> castTime,
                             Optional<Integer> manaCost, boolean continuousCost, int costInterval,
                             Optional<Double> raycastDistance, SpellCastInterruptMode interruptMode) {
            this.spell = spell;
            this.castTime = castTime;
            this.manaCost = manaCost;
            this.continuousCost = continuousCost;
            this.costInterval = costInterval;
            this.raycastDistance = raycastDistance;
            this.interruptMode = interruptMode;
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

        public Optional<Double> raycastDistance() {
            return raycastDistance;
        }

        public SpellCastInterruptMode interruptMode() {
            return interruptMode;
        }
    }
}
