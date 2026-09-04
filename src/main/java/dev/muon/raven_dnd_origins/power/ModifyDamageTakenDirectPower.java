package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.DamageTakenPowerConditions;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies its modifiers to damage that already survived armour and resistances, unlike
 * {@code apoli:modify_damage_taken} which runs before them.
 */
public class ModifyDamageTakenDirectPower extends PowerType<ModifyDamageTakenDirectPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Configuration::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Configuration::modifiers),
            LoggedOptionalField.strict("damage_condition", DamageCondition.CODEC).forGetter(Configuration::damageCondition),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Configuration::biEntityCondition),
            LoggedOptionalField.of("self_action", EntityAction.CODEC).forGetter(Configuration::selfAction),
            LoggedOptionalField.of("attacker_action", EntityAction.CODEC).forGetter(Configuration::attackerAction),
            LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Configuration::biEntityAction),
            LoggedOptionalField.strict("apply_armor_condition", EntityCondition.CODEC).forGetter(Configuration::applyArmorCondition),
            LoggedOptionalField.strict("damage_armor_condition", EntityCondition.CODEC).forGetter(Configuration::damageArmorCondition)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static float modify(Entity entity, DamageSource source, float amount) {
        if (!(entity instanceof LivingEntity living) || living.level().isClientSide()) {
            return amount;
        }
        List<AttributeModifier> modifiers = new ArrayList<>();
        List<Configuration> matched = new ArrayList<>();
        PowerLookup.forEach(living, ModPowers.MODIFY_DAMAGE_TAKEN, Configuration.class, cfg -> {
            if (!matches(cfg, living, source, amount)) {
                return;
            }
            modifiers.addAll(cfg.allModifiers());
            matched.add(cfg);
        });
        if (matched.isEmpty()) {
            return amount;
        }
        float modified = Math.max(0.0F, AttributeModifierHelper.apply(amount, modifiers, living));
        for (Configuration cfg : matched) {
            runActions(cfg, living, source);
        }
        return modified;
    }

    /**
     * BiEntity convention: actor = power holder (entity taking damage), target = damage source (entity causing damage).
     * For projectile damage: (power holder, projectile). For melee: (power holder, attacker).
     */
    public static boolean matches(Configuration cfg, LivingEntity entity, DamageSource source, float amount) {
        return DamageTakenPowerConditions.matches(cfg.damageCondition(), cfg.biEntityCondition(), entity, source, amount);
    }

    private static void runActions(Configuration cfg, LivingEntity entity, DamageSource source) {
        Level level = entity.level();
        cfg.selfAction().ifPresent(action -> action.run(new EntityCtx(entity, level)));
        Entity attacker = source.getEntity();
        if (attacker == null) {
            return;
        }
        cfg.attackerAction().ifPresent(action -> action.run(new EntityCtx(attacker, level)));
        cfg.biEntityAction().ifPresent(action -> action.run(new BiEntityCtx(entity, attacker, level)));
    }

    public record Configuration(
            Optional<AttributeModifier> modifier,
            Optional<List<AttributeModifier>> modifiers,
            Optional<DamageCondition> damageCondition,
            Optional<BiEntityCondition> biEntityCondition,
            Optional<EntityAction> selfAction,
            Optional<EntityAction> attackerAction,
            Optional<BiEntityAction> biEntityAction,
            Optional<EntityCondition> applyArmorCondition,
            Optional<EntityCondition> damageArmorCondition
    ) {
        public List<AttributeModifier> allModifiers() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }
}
