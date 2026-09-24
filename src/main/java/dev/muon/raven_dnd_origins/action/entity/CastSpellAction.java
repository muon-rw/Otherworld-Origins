package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.util.spell.CastFinishCallbacks;
import dev.muon.raven_dnd_origins.util.spell.SpellCastInterruptMode;
import dev.muon.raven_dnd_origins.util.spell.SpellCastUtil;
import dev.muon.raven_dnd_origins.util.spell.SpellSelection;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class CastSpellAction implements ActionType<EntityCtx, CastSpellAction.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(ResourceLocation.CODEC, SpellSelection.OBJECT_CODEC)
                    .fieldOf("spell")
                    .forGetter(Configuration::spellFieldForCodec),
            Codec.INT.optionalFieldOf("power_level").forGetter(Configuration::legacyPowerLevelForCodec),
            Codec.INT.optionalFieldOf("cast_time").forGetter(Configuration::castTime),
            Codec.INT.optionalFieldOf("mana_cost").forGetter(Configuration::manaCost),
            Codec.BOOL.optionalFieldOf("continuous_cost", false).forGetter(Configuration::continuousCost),
            Codec.INT.optionalFieldOf("cost_interval", 20).forGetter(Configuration::costInterval),
            Codec.DOUBLE.optionalFieldOf("raycast_distance").forGetter(Configuration::raycastDistance),
            SpellCastInterruptMode.CODEC.optionalFieldOf("interrupt_mode", SpellCastInterruptMode.CANCEL).forGetter(Configuration::interruptMode),
            LoggedOptionalField.of("success_action", EntityAction.CODEC).forGetter(Configuration::successAction),
            LoggedOptionalField.of("finish_action", EntityAction.CODEC).forGetter(Configuration::finishAction)
    ).apply(instance, Configuration::create));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public void run(Configuration configuration, EntityCtx ctx) {
        if (!(ctx.entity() instanceof LivingEntity livingEntity)) {
            RavenDndOrigins.LOGGER.debug("CastSpellAction: entity is not a LivingEntity: {}", ctx.entity());
            return;
        }

        Level world = ctx.level();
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

        boolean cast;
        boolean consumesRecast = false;
        if (livingEntity instanceof ServerPlayer serverPlayer) {
            consumesRecast = magicData.getPlayerRecasts().hasRecastForSpell(spell.getSpellId());
            cast = SpellCastUtil.castSpellForPlayer(
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
            cast = true;
        } else if (spell.checkPreCastConditions(world, powerLevel, livingEntity, magicData)) {
            SpellCastUtil.maybeUpdateTargetData(livingEntity, raycastTarget, magicData, spell);
            spell.onCast(world, powerLevel, livingEntity, CastSource.COMMAND, magicData);
            spell.onServerCastComplete(world, powerLevel, livingEntity, magicData, false);
            cast = true;
        } else {
            cast = false;
        }

        // Recasts are free, matching Iron's, which never charges mana for them.
        if (!cast || consumesRecast) {
            return;
        }
        EntityCtx actionCtx = new EntityCtx(livingEntity, world);
        configuration.successAction().ifPresent(action -> action.run(actionCtx));
        configuration.finishAction().ifPresent(action -> {
            if (livingEntity instanceof ServerPlayer serverPlayer) {
                CastFinishCallbacks.runWhenFinished(serverPlayer, spell, player -> action.run(new EntityCtx(player, player.level())));
            } else {
                action.run(actionCtx);
            }
        });
    }

    public record Configuration(
            SpellSelection spell,
            Optional<Integer> castTime,
            Optional<Integer> manaCost,
            boolean continuousCost,
            int costInterval,
            Optional<Double> raycastDistance,
            SpellCastInterruptMode interruptMode,
            Optional<EntityAction> successAction,
            Optional<EntityAction> finishAction
    ) {
        private static Configuration create(
                Either<ResourceLocation, SpellSelection> spellField,
                Optional<Integer> legacyPowerLevel,
                Optional<Integer> castTime,
                Optional<Integer> manaCost,
                boolean continuousCost,
                int costInterval,
                Optional<Double> raycastDistance,
                SpellCastInterruptMode interruptMode,
                Optional<EntityAction> successAction,
                Optional<EntityAction> finishAction
        ) {
            SpellSelection selection = spellField.map(
                    rl -> SpellSelection.fromLegacyStringForm(rl, legacyPowerLevel.orElse(1)),
                    s -> s
            );
            return new Configuration(selection, castTime, manaCost, continuousCost, costInterval, raycastDistance, interruptMode, successAction, finishAction);
        }

        private Either<ResourceLocation, SpellSelection> spellFieldForCodec() {
            return spell.encodeForActionSpellField();
        }

        private Optional<Integer> legacyPowerLevelForCodec() {
            return spell.legacyPowerLevelForCodec();
        }
    }
}
