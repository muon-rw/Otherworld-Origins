package dev.muon.otherworldorigins.power;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.ActionOnAttackRecursionGuard;
import io.github.apace100.apoli.util.HudRender;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.MustBeBound;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.power.ICooldownPowerConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.power.CooldownPowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.ComboState;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerAttackProperties;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/**
 * Fires a bi-entity action after a successful player melee hit ({@link Player#attack} dealt damage).
 * Injected at the end of vanilla's successful-hit path so weapon damage, knockback, sweep, and
 * enchantment post-effects run first. Spell and projectile damage never trigger this.
 * <p>
 * While the configured action runs, the target's {@link LivingEntity#invulnerableTime} is cleared
 * and then restored so bonus {@code origins:damage}
 * in the action is not swallowed by i-frames from the melee hit.
 * <p>
 * Optional {@code attack_condition} ({@link AttackCondition}) gates the swing and optionally requires
 * non-null Better Combat weapon attributes on the held item.
 * <p>
 * Nested {@code Player#attack} calls from {@link dev.muon.otherworldorigins.action.bientity.AttackAction}
 * are ignored via {@link ActionOnAttackRecursionGuard} so powers do not re-enter on synthetic swings.
 */
public class ActionOnAttackPower extends CooldownPowerFactory.Simple<ActionOnAttackPower.Configuration> {

    public ActionOnAttackPower() {
        super(Configuration.CODEC);
    }

    /**
     * Called from a mixin at the tail of {@link Player#attack}'s successful-hit branch (after melee damage and related effects).
     */
    public static void afterSuccessfulPlayerMeleeHit(Player attacker, Entity target) {
        if (attacker.level().isClientSide()) {
            return;
        }
        if (ActionOnAttackRecursionGuard.isNestedPlayerAttack()) {
            return;
        }
        IPowerContainer.getPowers(attacker, ModPowers.ACTION_ON_ATTACK.get())
                .forEach(powerHolder -> {
                    ActionOnAttackPower factory = powerHolder.value().getFactory();
                    factory.onAttack(powerHolder.value(), attacker, target);
                });
    }

    private void onAttack(ConfiguredPower<Configuration, ?> power, Player attacker, Entity target) {
        Configuration config = power.getConfiguration();
        if (!AttackCondition.allows(config.attackCondition(), attacker)) {
            return;
        }
        if (this.canUse(power, attacker)
                && ConfiguredBiEntityCondition.check(config.biEntityCondition(), attacker, target)) {
            runBiEntityActionClearingTargetIFrames(target, () ->
                    ConfiguredBiEntityAction.execute(config.biEntityAction(), attacker, target));
            this.use(power, attacker);
        }
    }

    /**
     * Temporarily clears the target's hurt invulnerability so damage inside the bi-entity action can apply,
     * then restores the previous timer (Apotheosis-style).
     */
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
            Optional<AttackCondition> attackCondition
    ) implements ICooldownPowerConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "cooldown", 1).forGetter(Configuration::duration),
                CalioCodecHelper.optionalField(HudRender.CODEC, "hud_render", HudRender.DONT_RENDER).forGetter(Configuration::hudRender),
                ConfiguredBiEntityCondition.optional("bientity_condition").forGetter(Configuration::biEntityCondition),
                ConfiguredBiEntityAction.required("bientity_action").forGetter(Configuration::biEntityAction),
                AttackCondition.CODEC.optionalFieldOf("attack_condition").forGetter(Configuration::attackCondition)
        ).apply(instance, Configuration::new));
    }

    /**
     * Datapack {@code attack_condition}: which combo swing counts, and whether the held item must have
     * non-null Better Combat {@link net.bettercombat.api.WeaponAttributes} (via {@link WeaponRegistry#getAttributes}).
     * <p>
     * JSON may be a string ({@code "first"} / {@code "final"}) — treated as {@code require_weapon_attributes: true} —
     * or an object {@code { "swing", "require_weapon_attributes" }} (default {@code true} when omitted).
     * When the field is absent from the power JSON, no swing or weapon gating applies.
     */
    public record AttackCondition(Swing swing, boolean requireWeaponAttributes) {

        private static final Codec<AttackCondition> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Swing.CODEC.fieldOf("swing").forGetter(AttackCondition::swing),
                Codec.BOOL.optionalFieldOf("require_weapon_attributes", true).forGetter(AttackCondition::requireWeaponAttributes)
        ).apply(instance, AttackCondition::new));

        /**
         * Object form is tried first on decode; a bare {@code "first"} / {@code "final"} string maps to
         * {@code require_weapon_attributes == true}. Encodes as the object form (same datapack semantics).
         */
        public static final Codec<AttackCondition> CODEC = Codec.either(OBJECT_CODEC, Swing.CODEC).xmap(
                either -> either.map(Function.identity(), swing -> new AttackCondition(swing, true)),
                Either::left
        );

        public static boolean allows(Optional<AttackCondition> condition, Player player) {
            if (condition.isEmpty()) {
                return true;
            }
            AttackCondition c = condition.get();
            AttackHand hand = currentAttackHand(player);
            if (c.requireWeaponAttributes && ModList.get().isLoaded("bettercombat")) {
                if (hand == null) {
                    return false;
                }
                if (WeaponRegistry.getAttributes(hand.itemStack()) == null) {
                    return false;
                }
            }
            return c.swing().matches(hand);
        }

        @Nullable
        private static AttackHand currentAttackHand(Player player) {
            if (!(player instanceof PlayerAttackProperties props)) {
                return null;
            }
            return PlayerAttackHelper.getCurrentAttack(player, props.getComboCount());
        }

        /**
         * First vs final swing in a Better Combat combo (same index rules as Better Combat).
         */
        public enum Swing implements StringRepresentable {
            /**
             * First attack in the sequence, i.e. {@link ComboState#current()} {@code == 1}.
             */
            FIRST("first"),
            /**
             * Last attack in the sequence, i.e. {@link ComboState#current()} {@code ==} {@link ComboState#total()}.
             */
            FINAL("final");

            public static final Codec<Swing> CODEC = StringRepresentable.fromEnum(Swing::values);

            private final String serializedName;

            Swing(String serializedName) {
                this.serializedName = serializedName;
            }

            @Override
            public String getSerializedName() {
                return serializedName;
            }

            /**
             * @param hand {@code null} when not using Better Combat weapon attributes (e.g. bare fist);
             *             treated as a single-attack sequence so both {@link #FIRST} and {@link #FINAL} match.
             */
            public boolean matches(@Nullable AttackHand hand) {
                if (hand == null) {
                    return true;
                }
                ComboState combo = hand.combo();
                int total = combo.total();
                if (total <= 0) {
                    return false;
                }
                int current = combo.current();
                return switch (this) {
                    case FIRST -> current == 1;
                    case FINAL -> current == total;
                };
            }
        }
    }
}
