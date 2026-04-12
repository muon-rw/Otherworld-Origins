package dev.muon.otherworldorigins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.util.spell.RecentSpellCastCache;
import dev.muon.otherworldorigins.util.spell.SpellCastInterruptMode;
import dev.muon.otherworldorigins.util.spell.SpellCastUtil;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import java.util.Optional;

/**
 * Steals an Iron's Spellbooks cast from the target for the actor. If the target is currently casting,
 * their cast is cancelled first. Otherwise the most recent completed cast from {@link RecentSpellCastCache}
 * is used. Optionally puts that spell on cooldown for the victim and drains their mana (players only for
 * mana). The stolen cast uses the same pipeline as {@link CastSpellBientityAction} (target = victim).
 * If {@code interrupt_mode} is {@code fail} and the actor is already casting, the action aborts before the target is modified.
 */
public class SpellThiefBientityAction extends BiEntityAction<SpellThiefBientityAction.Configuration> {

    public SpellThiefBientityAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(actor instanceof LivingEntity caster)) {
            OtherworldOrigins.LOGGER.debug("SpellThiefBientityAction: Actor is not a LivingEntity: {}", actor);
            return;
        }
        if (!(target instanceof LivingEntity livingTarget)) {
            OtherworldOrigins.LOGGER.debug("SpellThiefBientityAction: Target is not a LivingEntity: {}", target);
            return;
        }

        Level world = actor.level();
        if (world.isClientSide) {
            return;
        }

        if (MinecraftForge.EVENT_BUS.post(new CounterSpellEvent(actor, target))) {
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
        } else {
            if (stolenSpell.checkPreCastConditions(world, stolenLevel, caster, casterMagicData)) {
                SpellCastUtil.maybeApplyBientityProvidedTarget(caster, livingTarget, casterMagicData, stolenSpell);
                stolenSpell.onCast(world, stolenLevel, caster, CastSource.COMMAND, casterMagicData);
                stolenSpell.onServerCastComplete(world, stolenLevel, caster, casterMagicData, false);
            }
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
     * Puts {@code spell} on cooldown for the victim, matching {@link MagicManager#addCooldown(ServerPlayer, AbstractSpell, CastSource)}
     * for players (events + sync). Non-players use the same effective cooldown formula without packet sync.
     */
    private static void refreshVictimSpellCooldown(LivingEntity victim, AbstractSpell spell) {
        if (victim instanceof ServerPlayer serverPlayer) {
            MagicHelper.MAGIC_MANAGER.addCooldown(serverPlayer, spell, CastSource.COMMAND);
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
        double reduction = victim.getAttributeValue(AttributeRegistry.COOLDOWN_REDUCTION.get());
        return (int) (spell.getSpellCooldown() * (2 - Utils.softCapFormula(reduction)));
    }

    public static class Configuration implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("trigger_target_cooldown", true).forGetter(Configuration::triggerTargetCooldown),
                Codec.INT.optionalFieldOf("cast_time").forGetter(Configuration::castTime),
                Codec.INT.optionalFieldOf("mana_cost").forGetter(Configuration::manaCost),
                Codec.BOOL.optionalFieldOf("continuous_cost", false).forGetter(Configuration::continuousCost),
                Codec.INT.optionalFieldOf("cost_interval", 20).forGetter(Configuration::costInterval),
                Codec.INT.optionalFieldOf("target_mana_cost").forGetter(Configuration::targetManaCost),
                SpellCastInterruptMode.CODEC.optionalFieldOf("interrupt_mode", SpellCastInterruptMode.CANCEL).forGetter(Configuration::interruptMode)
        ).apply(instance, Configuration::create));

        private final boolean triggerTargetCooldown;
        private final Optional<Integer> castTime;
        private final Optional<Integer> manaCost;
        private final boolean continuousCost;
        private final int costInterval;
        private final Optional<Integer> targetManaCost;
        private final SpellCastInterruptMode interruptMode;

        private static Configuration create(
                boolean triggerTargetCooldown,
                Optional<Integer> castTime,
                Optional<Integer> manaCost,
                boolean continuousCost,
                int costInterval,
                Optional<Integer> targetManaCost,
                SpellCastInterruptMode interruptMode
        ) {
            return new Configuration(triggerTargetCooldown, castTime, manaCost, continuousCost, costInterval, targetManaCost, interruptMode);
        }

        public Configuration(boolean triggerTargetCooldown, Optional<Integer> castTime, Optional<Integer> manaCost,
                             boolean continuousCost, int costInterval, Optional<Integer> targetManaCost,
                             SpellCastInterruptMode interruptMode) {
            this.triggerTargetCooldown = triggerTargetCooldown;
            this.castTime = castTime;
            this.manaCost = manaCost;
            this.continuousCost = continuousCost;
            this.costInterval = costInterval;
            this.targetManaCost = targetManaCost;
            this.interruptMode = interruptMode;
        }

        public boolean triggerTargetCooldown() {
            return triggerTargetCooldown;
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

        public Optional<Integer> targetManaCost() {
            return targetManaCost;
        }

        public SpellCastInterruptMode interruptMode() {
            return interruptMode;
        }
    }
}
