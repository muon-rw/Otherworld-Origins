package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.ActionOnSpellDamageRecursionGuard;
import io.github.apace100.apoli.util.HudRender;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.MustBeBound;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.power.ICooldownPowerConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.power.CooldownPowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Fires a bi-entity action after Iron's spell damage has been applied to the target's health: MixinExtras {@code WrapOperation}
 * on {@link LivingEntity#setHealth} in {@link LivingEntity#actuallyHurt} calls {@code original.call} first, then this hook
 * when the {@link DamageSource} is a {@link SpellDamageSource} (same timing idea as {@link ActionOnAttackPower} after a successful melee {@code hurt}).
 * <p>
 * Actor is {@link SpellDamageSource#getEntity()} (spell attacker); target is the hurt {@link LivingEntity}.
 * Only {@link Player} actors are considered, matching {@link ActionOnAttackPower}'s player-only scope.
 * <p>
 * Optional {@code spell_conditions} uses the same fields as {@link CastSpellConditions}, but only
 * {@code spell}, {@code spells}, {@code spell_tag}, and {@code spell_school} apply; cast source/type are ignored.
 * <p>
 * While the configured action runs, the target's {@link LivingEntity#invulnerableTime} is cleared and restored
 * so bonus {@code origins:damage} in the action is not swallowed by i-frames from the triggering hit.
 */
public class ActionOnSpellDamagePower extends CooldownPowerFactory.Simple<ActionOnSpellDamagePower.Configuration> {

    public ActionOnSpellDamagePower() {
        super(Configuration.CODEC);
    }

    /**
     * Invoked from {@link dev.muon.otherworldorigins.mixin.LivingEntityMixin} after {@code original.call} for
     * {@link LivingEntity#setHealth} in {@link LivingEntity#actuallyHurt}.
     */
    public static void afterSpellDamageApplied(LivingEntity target, SpellDamageSource spellSource) {
        if (target.level().isClientSide()) {
            return;
        }
        if (ActionOnSpellDamageRecursionGuard.isNestedSpellDamageAction()) {
            return;
        }
        Entity attackerEntity = spellSource.getEntity();
        if (!(attackerEntity instanceof Player attacker)) {
            return;
        }
        AbstractSpell spell = spellSource.spell();
        if (spell == null) {
            return;
        }
        IPowerContainer.getPowers(attacker, ModPowers.ACTION_ON_SPELL_DAMAGE.get()).forEach(holder -> {
            ActionOnSpellDamagePower factory = holder.value().getFactory();
            factory.onSpellDamage(holder.value(), attacker, target, spell);
        });
    }

    private void onSpellDamage(ConfiguredPower<Configuration, ?> power, Player attacker, Entity target, AbstractSpell spell) {
        Configuration config = power.getConfiguration();
        if (!config.spellConditions().matchesSpellOnly(spell)) {
            return;
        }
        if (this.canUse(power, attacker)
                && ConfiguredBiEntityCondition.check(config.biEntityCondition(), attacker, target)) {
            runBiEntityActionClearingTargetIFrames(target, () ->
                    ActionOnSpellDamageRecursionGuard.runNested(() ->
                            ConfiguredBiEntityAction.execute(config.biEntityAction(), attacker, target)));
            this.use(power, attacker);
        }
    }

    private static void runBiEntityActionClearingTargetIFrames(Entity target, Runnable action) {
        if (!(target instanceof LivingEntity living)) {
            action.run();
            return;
        }
        int saved = living.invulnerableTime;
        living.invulnerableTime = 0;
        try {
            action.run();
        } finally {
            living.invulnerableTime = saved;
        }
    }

    public record Configuration(
            int duration,
            HudRender hudRender,
            Holder<ConfiguredBiEntityCondition<?, ?>> biEntityCondition,
            @MustBeBound Holder<ConfiguredBiEntityAction<?, ?>> biEntityAction,
            CastSpellConditions spellConditions
    ) implements ICooldownPowerConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "cooldown", 1).forGetter(Configuration::duration),
                CalioCodecHelper.optionalField(HudRender.CODEC, "hud_render", HudRender.DONT_RENDER).forGetter(Configuration::hudRender),
                ConfiguredBiEntityCondition.optional("bientity_condition").forGetter(Configuration::biEntityCondition),
                ConfiguredBiEntityAction.required("bientity_action").forGetter(Configuration::biEntityAction),
                CastSpellConditions.CODEC.optionalFieldOf("spell_conditions", CastSpellConditions.defaults())
                        .forGetter(Configuration::spellConditions)
        ).apply(instance, Configuration::new));
    }
}
