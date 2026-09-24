package dev.muon.raven_dnd_origins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.util.spell.RecentSpellCastCache;
import dev.muon.raven_dnd_origins.util.spell.SpellCastInterruptMode;
import dev.muon.raven_dnd_origins.util.spell.SpellCastUtil;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

/**
 * Steals a spell cast from the target for the actor. If the target is currently casting, their cast is cancelled
 * first; otherwise the most recent completed cast from {@link RecentSpellCastCache} is used. Optionally puts that
 * spell on cooldown for the victim and drains their mana (players only for mana). The stolen cast uses the same
 * pipeline as {@link CastSpellBientityAction} (target = victim). If {@code interrupt_mode} is {@code fail} and the
 * actor is already casting, the action aborts before the target is modified.
 */
public final class SpellThiefBientityAction implements ActionType<BiEntityCtx, SpellThiefBientityAction.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("trigger_target_cooldown", true).forGetter(Configuration::triggerTargetCooldown),
            Codec.INT.optionalFieldOf("cast_time").forGetter(Configuration::castTime),
            Codec.INT.optionalFieldOf("mana_cost").forGetter(Configuration::manaCost),
            Codec.BOOL.optionalFieldOf("continuous_cost", false).forGetter(Configuration::continuousCost),
            Codec.INT.optionalFieldOf("cost_interval", 20).forGetter(Configuration::costInterval),
            Codec.INT.optionalFieldOf("target_mana_cost").forGetter(Configuration::targetManaCost),
            SpellCastInterruptMode.CODEC.optionalFieldOf("interrupt_mode", SpellCastInterruptMode.CANCEL).forGetter(Configuration::interruptMode)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public void run(Configuration configuration, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof LivingEntity caster)) {
            RavenDndOrigins.LOGGER.debug("SpellThiefBientityAction: Actor is not a LivingEntity: {}", ctx.actor());
            return;
        }
        if (!(ctx.target() instanceof LivingEntity livingTarget)) {
            RavenDndOrigins.LOGGER.debug("SpellThiefBientityAction: Target is not a LivingEntity: {}", ctx.target());
            return;
        }

        Level world = ctx.level();
        if (world.isClientSide) {
            return;
        }

        if (NeoForge.EVENT_BUS.post(new CounterSpellEvent(caster, livingTarget)).isCanceled()) {
            return;
        }

        if (configuration.interruptMode() == SpellCastInterruptMode.FAIL
                && MagicData.getPlayerMagicData(caster).isCasting()) {
            if (caster instanceof ServerPlayer serverPlayer) {
                SpellCastUtil.sendCannotUseWhileCastingActionBar(serverPlayer);
            }
            return;
        }

        MagicData victimData = MagicData.getPlayerMagicData(livingTarget);
        AbstractSpell stolenSpell;
        int stolenLevel;

        if (victimData.isCasting()) {
            stolenSpell = victimData.getCastingSpell().getSpell();
            if (stolenSpell == SpellRegistry.none()) {
                return;
            }
            stolenLevel = victimData.getCastingSpellLevel();

            if (livingTarget instanceof ServerPlayer victimPlayer) {
                Utils.serverSideCancelCast(victimPlayer, configuration.triggerTargetCooldown());
            } else if (livingTarget instanceof IMagicEntity magicVictim) {
                magicVictim.cancelCast();
            } else {
                victimData.resetCastingState();
            }
        } else {
            Optional<RecentSpellCastCache.ResolvedLastCast> last = RecentSpellCastCache.getLastCast(livingTarget, world);
            if (last.isEmpty()) {
                return;
            }
            RecentSpellCastCache.ResolvedLastCast resolved = last.get();
            stolenSpell = resolved.spell();
            stolenLevel = resolved.level();
        }

        applyVictimCosts(configuration, livingTarget, stolenSpell);

        MagicData casterMagicData = MagicData.getPlayerMagicData(caster);
        casterMagicData = SpellCastUtil.resolveBusyCastBeforeNewSpell(caster, world, casterMagicData, configuration.interruptMode(), stolenSpell);
        if (casterMagicData == null) {
            return;
        }

        if (caster instanceof ServerPlayer serverPlayer) {
            SpellCastUtil.castSpellForPlayerWithBientityTarget(
                    stolenSpell,
                    stolenLevel,
                    serverPlayer,
                    casterMagicData,
                    world,
                    livingTarget,
                    configuration.castTime(),
                    configuration.manaCost(),
                    configuration.continuousCost(),
                    configuration.costInterval()
            );
        } else if (caster instanceof IMagicEntity magicEntity) {
            magicEntity.initiateCastSpell(stolenSpell, stolenLevel);
        } else if (stolenSpell.checkPreCastConditions(world, stolenLevel, caster, casterMagicData)) {
            SpellCastUtil.maybeApplyBientityProvidedTarget(caster, livingTarget, casterMagicData, stolenSpell);
            stolenSpell.onCast(world, stolenLevel, caster, CastSource.COMMAND, casterMagicData);
            stolenSpell.onServerCastComplete(world, stolenLevel, caster, casterMagicData, false);
        }
    }

    private static void applyVictimCosts(Configuration configuration, LivingEntity victim, AbstractSpell stolenSpell) {
        if (configuration.triggerTargetCooldown()) {
            refreshVictimSpellCooldown(victim, stolenSpell);
        }
        configuration.targetManaCost().ifPresent(cost -> {
            if (cost > 0 && victim instanceof ServerPlayer victimPlayer) {
                SpellCastUtil.drainPlayerMana(victimPlayer, cost);
            }
        });
    }

    /**
     * Players go through {@link MagicManager#addCooldown} for events and sync; non-players get the same effective
     * cooldown without packet sync.
     */
    private static void refreshVictimSpellCooldown(LivingEntity victim, AbstractSpell spell) {
        if (victim instanceof ServerPlayer serverPlayer) {
            SpellCastUtil.addCommandCooldown(serverPlayer, spell);
        } else {
            int ticks = effectiveSpellCooldownTicks(victim, spell);
            if (ticks > 0) {
                MagicData.getPlayerMagicData(victim).getPlayerCooldowns().addCooldown(spell, ticks);
            }
        }
    }

    private static int effectiveSpellCooldownTicks(LivingEntity victim, AbstractSpell spell) {
        if (victim instanceof Player player) {
            return MagicManager.getEffectiveSpellCooldown(spell, player, CastSource.COMMAND);
        }
        double reduction = victim.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION);
        return (int) (spell.getSpellCooldown() * (2 - Utils.softCapFormula(reduction)));
    }

    public record Configuration(
            boolean triggerTargetCooldown,
            Optional<Integer> castTime,
            Optional<Integer> manaCost,
            boolean continuousCost,
            int costInterval,
            Optional<Integer> targetManaCost,
            SpellCastInterruptMode interruptMode
    ) {
    }
}
