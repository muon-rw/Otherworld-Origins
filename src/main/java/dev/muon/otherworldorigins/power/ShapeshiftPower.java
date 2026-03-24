package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class ShapeshiftPower extends PowerFactory<ShapeshiftPower.Configuration> {

    public ShapeshiftPower() {
        super(Configuration.CODEC);
    }

    @Nullable
    public static Configuration getActiveShapeshiftConfig(LivingEntity entity) {
        if (entity == null) return null;
        return IPowerContainer.get(entity).resolve()
                .stream()
                .flatMap(container -> container.getPowers(ModPowers.SHAPESHIFT.get()).stream())
                .map(holder -> holder.value().getConfiguration())
                .findFirst()
                .orElse(null);
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
            double upswing
    ) {
        public static final Codec<ShapeshiftAttack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CalioCodecHelper.optionalField(Codec.STRING, "hitbox", "HORIZONTAL_PLANE").forGetter(ShapeshiftAttack::hitbox),
                CalioCodecHelper.optionalField(Codec.DOUBLE, "angle", 100.0).forGetter(ShapeshiftAttack::angle),
                CalioCodecHelper.optionalField(Codec.DOUBLE, "damage_multiplier", 1.0).forGetter(ShapeshiftAttack::damageMultiplier),
                CalioCodecHelper.optionalField(Codec.DOUBLE, "upswing", 0.5).forGetter(ShapeshiftAttack::upswing)
        ).apply(instance, ShapeshiftAttack::new));
    }

    public record Configuration(
            ResourceLocation entityType,
            boolean hideHands,
            boolean allowTools,
            float playAttackSoundChance,
            boolean preventEquipment,
            Holder<ConfiguredEntityCondition<?, ?>> preventSpellCasts,
            double attackRange,
            List<ShapeshiftAttack> attacks
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("entity_type").forGetter(Configuration::entityType),
                CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "hide_hands", true).forGetter(Configuration::hideHands),
                CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "allow_tools", false).forGetter(Configuration::allowTools),
                CalioCodecHelper.optionalField(Codec.FLOAT, "play_attack_sound_chance", 0.2F).forGetter(Configuration::playAttackSoundChance),
                CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "prevent_equipment", true).forGetter(Configuration::preventEquipment),
                ConfiguredEntityCondition.optional("prevent_spell_casts").forGetter(Configuration::preventSpellCasts),
                CalioCodecHelper.optionalField(Codec.DOUBLE, "attack_range", 0.0).forGetter(Configuration::attackRange),
                CalioCodecHelper.optionalField(ShapeshiftAttack.CODEC.listOf(), "attacks", List.of()).forGetter(Configuration::attacks)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return entityType != null && BuiltInRegistries.ENTITY_TYPE.containsKey(entityType);
        }

        public boolean hasAttackOverrides() {
            return !attacks.isEmpty() || attackRange > 0;
        }

        /**
         * Compares only the syncable/primitive fields, ignoring Holder-based condition
         * fields which lack meaningful equals and would cause spurious sync broadcasts.
         */
        public boolean syncFieldsEqual(@Nullable Configuration other) {
            if (other == null) return false;
            return Objects.equals(entityType, other.entityType)
                    && hideHands == other.hideHands
                    && allowTools == other.allowTools
                    && Float.compare(playAttackSoundChance, other.playAttackSoundChance) == 0
                    && preventEquipment == other.preventEquipment;
        }
    }
}
