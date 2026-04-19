package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Stamps potions brewed by the holder with bonus duration/amplifier per effect category.
 * Applied at brewing-stand brew completion via {@code BrewingStandBlockEntityMixin}.
 */
public class ModifyBrewedPotionPower extends PowerFactory<ModifyBrewedPotionPower.Configuration> {

    public ModifyBrewedPotionPower() {
        super(Configuration.CODEC);
    }

    public static boolean appliesTo(Configuration config, MobEffectCategory category) {
        return (config.affectBeneficial && category == MobEffectCategory.BENEFICIAL)
                || (config.affectHarmful && category == MobEffectCategory.HARMFUL)
                || (config.affectNeutral && category == MobEffectCategory.NEUTRAL);
    }

    public record Configuration(
            boolean affectBeneficial,
            boolean affectHarmful,
            boolean affectNeutral,
            float durationMultiplier,
            int amplifierModifier
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("affect_beneficial", true).forGetter(Configuration::affectBeneficial),
                Codec.BOOL.optionalFieldOf("affect_harmful", true).forGetter(Configuration::affectHarmful),
                Codec.BOOL.optionalFieldOf("affect_neutral", true).forGetter(Configuration::affectNeutral),
                Codec.FLOAT.optionalFieldOf("duration_multiplier", 1.0f).forGetter(Configuration::durationMultiplier),
                Codec.INT.optionalFieldOf("amplifier_modifier", 0).forGetter(Configuration::amplifierModifier)
        ).apply(instance, Configuration::new));
    }
}
