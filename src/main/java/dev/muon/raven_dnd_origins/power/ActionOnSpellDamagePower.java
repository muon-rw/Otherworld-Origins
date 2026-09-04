package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.ActionOnSpellDamageRecursionGuard;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Fires a bi-entity action after Iron's spell damage has been applied to the target's health, from a
 * {@code WrapOperation} on {@link LivingEntity#setHealth} in {@code LivingEntity#actuallyHurt} (same timing
 * idea as {@link ActionOnAttackPower} after a successful melee {@code hurt}).
 * <p>
 * Actor is {@link SpellDamageSource#getEntity()} (spell attacker); target is the hurt {@link LivingEntity}.
 * Only {@link Player} actors are considered, matching {@link ActionOnAttackPower}'s player-only scope.
 * <p>
 * Optional {@code spell_conditions} uses the same fields as {@link CastSpellConditions}, but only
 * {@code spell}, {@code spells}, {@code spell_tag}, and {@code spell_school} apply; cast source/type are ignored.
 * <p>
 * While the configured action runs, the target's {@link LivingEntity#invulnerableTime} is cleared and restored
 * so bonus {@code apoli:damage} in the action is not swallowed by i-frames from the triggering hit.
 */
public class ActionOnSpellDamagePower extends PowerType<ActionOnSpellDamagePower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Expression.INT_OR_EXPR.optionalFieldOf("cooldown", Expression.constant(1)).forGetter(Configuration::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Configuration::hudRender),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Configuration::bientityCondition),
            BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Configuration::bientityAction),
            CastSpellConditions.CODEC.optionalFieldOf("spell_conditions", CastSpellConditions.defaults())
                    .forGetter(Configuration::spellConditions)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Configuration cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && impl.getAuxInt(powerId).isEmpty()) {
            impl.setAuxInt(powerId, 0);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Configuration cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && !holder.hasPower(powerId)) {
            impl.removeAux(powerId);
        }
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, Configuration cfg, PowerContainer holder) {
        return PowerResources.readDeadline(holder, powerId);
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Configuration cfg, PowerContainer holder, int value) {
        return PowerResources.writeDeadline(holder, powerId, value, PowerResources.cooldownTicks(cfg.cooldown(), holder));
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Configuration cfg, PowerContainer holder, boolean max) {
        return OptionalInt.of(max ? Math.max(PowerResources.cooldownTicks(cfg.cooldown(), holder), 0) : 0);
    }

    /**
     * Invoked from {@link dev.muon.raven_dnd_origins.mixin.LivingEntityMixin} after {@code original.call} for
     * {@link LivingEntity#setHealth} in {@code LivingEntity#actuallyHurt}.
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
        if (!(PowerContainer.of(attacker) instanceof PowerContainerImpl impl)) {
            return;
        }
        long now = attacker.level().getGameTime();
        PowerLookup.forEachEntry(attacker, ModPowers.ACTION_ON_SPELL_DAMAGE, Configuration.class, (powerId, config) -> {
            if (!config.spellConditions().matchesSpellOnly(spell)) {
                return;
            }
            if (impl.getAuxIntOr(powerId, 0) > (int) now) {
                return;
            }
            BiEntityCtx ctx = new BiEntityCtx(attacker, target, attacker.level());
            if (config.bientityCondition().isPresent() && !config.bientityCondition().get().test(ctx)) {
                return;
            }
            runClearingTargetIFrames(target, () ->
                    ActionOnSpellDamageRecursionGuard.runNested(() -> config.bientityAction().run(ctx)));
            impl.setAuxInt(powerId, (int) now + Math.max(PowerResources.cooldownTicks(config.cooldown(), impl), 0));
        });
    }

    private static void runClearingTargetIFrames(LivingEntity target, Runnable action) {
        int saved = target.invulnerableTime;
        target.invulnerableTime = 0;
        try {
            action.run();
        } finally {
            target.invulnerableTime = saved;
        }
    }

    public record Configuration(
            Expression cooldown,
            HudRender hudRender,
            Optional<BiEntityCondition> bientityCondition,
            BiEntityAction bientityAction,
            CastSpellConditions spellConditions
    ) {
    }
}
