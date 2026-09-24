package dev.muon.raven_dnd_origins.action.bientity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.util.spell.BientitySpellCastAim;
import dev.muon.raven_dnd_origins.util.spell.SpellCastInterruptMode;
import dev.muon.raven_dnd_origins.util.spell.SpellCastUtil;
import dev.muon.raven_dnd_origins.util.spell.SpellSelection;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Casts from the actor at the bi-entity target. Unlike {@link dev.muon.raven_dnd_origins.action.entity.CastSpellAction}
 * there is no raycast: the target is provided. When the actor is already casting, {@code interrupt_mode} decides
 * between cancelling, force-completing, and aborting the action.
 */
public final class CastSpellBientityAction implements ActionType<BiEntityCtx, CastSpellBientityAction.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.either(ResourceLocation.CODEC, SpellSelection.OBJECT_CODEC)
                    .fieldOf("spell")
                    .forGetter(Configuration::spellFieldForCodec),
            Codec.INT.optionalFieldOf("power_level").forGetter(Configuration::legacyPowerLevelForCodec),
            Codec.INT.optionalFieldOf("cast_time").forGetter(Configuration::castTime),
            Codec.INT.optionalFieldOf("mana_cost").forGetter(Configuration::manaCost),
            Codec.BOOL.optionalFieldOf("continuous_cost", false).forGetter(Configuration::continuousCost),
            Codec.INT.optionalFieldOf("cost_interval", 20).forGetter(Configuration::costInterval),
            SpellCastInterruptMode.CODEC.optionalFieldOf("interrupt_mode", SpellCastInterruptMode.CANCEL).forGetter(Configuration::interruptMode),
            LoggedOptionalField.of("success_action", BiEntityAction.CODEC).forGetter(Configuration::successAction)
    ).apply(instance, Configuration::create));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public void run(Configuration configuration, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof LivingEntity caster)) {
            RavenDndOrigins.LOGGER.debug("CastSpellBientityAction: Actor is not a LivingEntity: {}", ctx.actor());
            return;
        }
        if (!(ctx.target() instanceof LivingEntity livingTarget)) {
            RavenDndOrigins.LOGGER.debug("CastSpellBientityAction: Target is not a LivingEntity: {}", ctx.target());
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
        MagicData magicData = MagicData.getPlayerMagicData(caster);
        magicData = SpellCastUtil.resolveBusyCastBeforeNewSpell(caster, world, magicData, configuration.interruptMode(), spell);
        if (magicData == null) {
            return;
        }

        boolean cast;
        boolean consumesRecast = false;
        if (caster instanceof ServerPlayer serverPlayer) {
            consumesRecast = magicData.getPlayerRecasts().hasRecastForSpell(spell.getSpellId());
            cast = SpellCastUtil.castSpellForPlayerWithBientityTarget(
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
            cast = true;
        } else {
            BientitySpellCastAim.push(caster, livingTarget);
            try {
                cast = spell.checkPreCastConditions(world, powerLevel, caster, magicData);
                if (cast) {
                    SpellCastUtil.maybeApplyBientityProvidedTarget(caster, livingTarget, magicData, spell);
                    spell.onCast(world, powerLevel, caster, CastSource.COMMAND, magicData);
                    spell.onServerCastComplete(world, powerLevel, caster, magicData, false);
                }
            } finally {
                BientitySpellCastAim.pop();
            }
        }

        // Recasts are free, matching Iron's, which never charges mana for them.
        if (cast && !consumesRecast) {
            configuration.successAction().ifPresent(action -> action.run(new BiEntityCtx(caster, livingTarget, world)));
        }
    }

    public record Configuration(
            SpellSelection spell,
            Optional<Integer> castTime,
            Optional<Integer> manaCost,
            boolean continuousCost,
            int costInterval,
            SpellCastInterruptMode interruptMode,
            Optional<BiEntityAction> successAction
    ) {
        private static Configuration create(
                Either<ResourceLocation, SpellSelection> spellField,
                Optional<Integer> legacyPowerLevel,
                Optional<Integer> castTime,
                Optional<Integer> manaCost,
                boolean continuousCost,
                int costInterval,
                SpellCastInterruptMode interruptMode,
                Optional<BiEntityAction> successAction
        ) {
            SpellSelection selection = spellField.map(
                    rl -> SpellSelection.fromLegacyStringForm(rl, legacyPowerLevel.orElse(1)),
                    s -> s
            );
            return new Configuration(selection, castTime, manaCost, continuousCost, costInterval, interruptMode, successAction);
        }

        private Either<ResourceLocation, SpellSelection> spellFieldForCodec() {
            return spell.encodeForActionSpellField();
        }

        private Optional<Integer> legacyPowerLevelForCodec() {
            return spell.legacyPowerLevelForCodec();
        }
    }
}
