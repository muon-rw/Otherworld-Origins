package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftCollisionShape;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ShapeshiftPower extends PowerType<ShapeshiftPower.Configuration> {

    @Override
    public MapCodec<Configuration> configCodec() {
        return Configuration.CODEC;
    }

    @Override
    public boolean isActive(ResourceLocation powerId, Configuration cfg, EntityCtx ctx) {
        return cfg.isConfigurationValid();
    }

    @Nullable
    public static Configuration getActiveShapeshiftConfig(LivingEntity entity) {
        if (entity == null) return null;
        PowerContainer container = PowerContainer.of(entity);
        if (container == null || container.isEmpty()) return null;
        List<ResourceLocation> powers = container.powersOfType(ModPowers.SHAPESHIFT);
        EntityCtx ctx = null;
        for (int i = 0; i < powers.size(); i++) {
            ResourceLocation powerId = powers.get(i);
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power == null) continue;
            if (!(power.config() instanceof Configuration config)) continue;
            if (!config.isConfigurationValid()) continue;
            if (power.condition().isPresent()) {
                if (ctx == null) ctx = EntityCtx.of(entity, entity.level());
                if (!power.condition().get().test(ctx)) continue;
            }
            return config;
        }
        return null;
    }

    @Nullable
    public static ResourceLocation getActiveShapeshiftType(LivingEntity entity) {
        Configuration config = getActiveShapeshiftConfig(entity);
        return config != null ? config.entityType() : null;
    }

    public static boolean isShapeshifted(LivingEntity entity) {
        return getActiveShapeshiftType(entity) != null;
    }

    /**
     * Defines a single attack in a Better Combat combo sequence.
     * When attacks are specified on the shapeshift power, they replace vanilla punching
     * with BC cone/sweep/AoE targeting while shapeshifted.
     */
    public record ShapeshiftAttack(
            String hitbox,
            double angle,
            double damageMultiplier,
            double upswing,
            String animation
    ) {
        public static final Codec<ShapeshiftAttack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("hitbox", "HORIZONTAL_PLANE").forGetter(ShapeshiftAttack::hitbox),
                Codec.DOUBLE.optionalFieldOf("angle", 100.0).forGetter(ShapeshiftAttack::angle),
                Codec.DOUBLE.optionalFieldOf("damage_multiplier", 1.0).forGetter(ShapeshiftAttack::damageMultiplier),
                Codec.DOUBLE.optionalFieldOf("upswing", 0.5).forGetter(ShapeshiftAttack::upswing),
                Codec.STRING.optionalFieldOf("animation", "").forGetter(ShapeshiftAttack::animation)
        ).apply(instance, ShapeshiftAttack::new));
    }

    public record Configuration(
            ResourceLocation entityType,
            boolean hideHands,
            boolean allowTools,
            float playAttackSoundChance,
            boolean suppressArmorModifiers,
            Optional<EntityCondition> preventSpellCasts,
            double attackRange,
            List<ShapeshiftAttack> attacks,
            float collisionWidth,
            float collisionHeight,
            boolean autoSwimInWater,
            float flightSpeed
    ) {

        /** Stable: ENTITY_TYPE registry is frozen at runtime, so containsKey results never change. */
        private static final ConcurrentHashMap<ResourceLocation, Boolean> ENTITY_TYPE_PRESENT = new ConcurrentHashMap<>();

        public static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("entity_type").forGetter(Configuration::entityType),
                Codec.BOOL.optionalFieldOf("hide_hands", true).forGetter(Configuration::hideHands),
                Codec.BOOL.optionalFieldOf("allow_tools", false).forGetter(Configuration::allowTools),
                Codec.FLOAT.optionalFieldOf("play_attack_sound_chance", 0.2F).forGetter(Configuration::playAttackSoundChance),
                Codec.BOOL.optionalFieldOf("suppress_armor_modifiers", true).forGetter(Configuration::suppressArmorModifiers),
                LoggedOptionalField.strict("prevent_spell_casts", EntityCondition.CODEC).forGetter(Configuration::preventSpellCasts),
                Codec.DOUBLE.optionalFieldOf("attack_range", 0.0).forGetter(Configuration::attackRange),
                ShapeshiftAttack.CODEC.listOf().optionalFieldOf("attacks", List.of()).forGetter(Configuration::attacks),
                Codec.FLOAT.optionalFieldOf("collision_width", -1.0F).forGetter(Configuration::collisionWidth),
                Codec.FLOAT.optionalFieldOf("collision_height", -1.0F).forGetter(Configuration::collisionHeight),
                Codec.BOOL.optionalFieldOf("auto_swim_in_water", false).forGetter(Configuration::autoSwimInWater),
                Codec.FLOAT.optionalFieldOf("flight_speed", 1.0F).forGetter(Configuration::flightSpeed)
        ).apply(instance, Configuration::new));

        public boolean isConfigurationValid() {
            if (entityType == null) return false;
            if (!ENTITY_TYPE_PRESENT.computeIfAbsent(entityType, BuiltInRegistries.ENTITY_TYPE::containsKey)) {
                return false;
            }
            boolean wSet = collisionWidth > 0.0F;
            boolean hSet = collisionHeight > 0.0F;
            return wSet == hSet;
        }

        public boolean hasAttackOverrides() {
            return !attacks.isEmpty() || attackRange > 0;
        }

        public boolean hasCollisionOverride() {
            return collisionWidth > 0.0F && collisionHeight > 0.0F;
        }

        /**
         * Hitbox used while this shapeshift is active: explicit {@code collision_*} when set, otherwise
         * {@link net.minecraft.world.entity.EntityType#getDimensions()} for {@link #entityType()}.
         */
        public ShapeshiftCollisionShape effectiveCollisionShape() {
            if (hasCollisionOverride()) {
                return new ShapeshiftCollisionShape(collisionWidth, collisionHeight);
            }
            EntityDimensions dims = BuiltInRegistries.ENTITY_TYPE.get(entityType).getDimensions();
            return new ShapeshiftCollisionShape(dims.width(), dims.height());
        }

        /**
         * Compares only the syncable/primitive fields, ignoring the condition field which has no
         * meaningful equals and would cause spurious sync broadcasts.
         */
        public boolean syncFieldsEqual(@Nullable Configuration other) {
            if (other == null) return false;
            return Objects.equals(entityType, other.entityType)
                    && hideHands == other.hideHands
                    && allowTools == other.allowTools
                    && Float.compare(playAttackSoundChance, other.playAttackSoundChance) == 0
                    && suppressArmorModifiers == other.suppressArmorModifiers
                    && Float.compare(collisionWidth, other.collisionWidth) == 0
                    && Float.compare(collisionHeight, other.collisionHeight) == 0
                    && Float.compare(flightSpeed, other.flightSpeed) == 0;
        }
    }
}
