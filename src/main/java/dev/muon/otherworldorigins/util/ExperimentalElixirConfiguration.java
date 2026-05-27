package dev.muon.otherworldorigins.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import net.minecraft.world.effect.MobEffectCategory;

import java.util.Locale;
import java.util.Optional;

public final class ExperimentalElixirConfiguration {
    private ExperimentalElixirConfiguration() {}

    public static final Codec<MobEffectCategory> CATEGORY_CODEC = Codec.STRING.comapFlatMap(
            s -> switch (s.toLowerCase(Locale.ROOT)) {
                case "beneficial" -> DataResult.success(MobEffectCategory.BENEFICIAL);
                case "harmful" -> DataResult.success(MobEffectCategory.HARMFUL);
                case "neutral" -> DataResult.success(MobEffectCategory.NEUTRAL);
                default -> DataResult.error(() -> "Unknown effect category '" + s + "'; expected beneficial, harmful, or neutral");
            },
            c -> switch (c) {
                case BENEFICIAL -> "beneficial";
                case HARMFUL -> "harmful";
                case NEUTRAL -> "neutral";
            }
    );

    public enum OutputMode {
        VANILLA_POTION,
        ARS_ELIXIR;

        public static final Codec<OutputMode> CODEC = Codec.STRING.comapFlatMap(
                s -> switch (s.toLowerCase(Locale.ROOT)) {
                    case "vanilla_potion" -> DataResult.success(VANILLA_POTION);
                    case "ars_elixir" -> DataResult.success(ARS_ELIXIR);
                    default -> DataResult.error(() -> "Unknown output_mode '" + s + "'; expected vanilla_potion or ars_elixir");
                },
                m -> switch (m) {
                    case VANILLA_POTION -> "vanilla_potion";
                    case ARS_ELIXIR -> "ars_elixir";
                }
        );
    }

    public record Configuration(
            MobEffectCategory category,
            Optional<Integer> amplifierMin,
            Optional<Integer> amplifierMax,
            Optional<Integer> durationMin,
            Optional<Integer> durationMax,
            boolean lingering,
            Optional<Integer> effectCountMin,
            Optional<Integer> effectCountMax,
            OutputMode outputMode,
            int masteryXp
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CATEGORY_CODEC.fieldOf("category").forGetter(Configuration::category),
                Codec.INT.optionalFieldOf("amplifier_min").forGetter(Configuration::amplifierMin),
                Codec.INT.optionalFieldOf("amplifier_max").forGetter(Configuration::amplifierMax),
                Codec.INT.optionalFieldOf("duration_min").forGetter(Configuration::durationMin),
                Codec.INT.optionalFieldOf("duration_max").forGetter(Configuration::durationMax),
                Codec.BOOL.optionalFieldOf("lingering", false).forGetter(Configuration::lingering),
                Codec.INT.optionalFieldOf("effect_count_min").forGetter(Configuration::effectCountMin),
                Codec.INT.optionalFieldOf("effect_count_max").forGetter(Configuration::effectCountMax),
                OutputMode.CODEC.optionalFieldOf("output_mode", OutputMode.VANILLA_POTION).forGetter(Configuration::outputMode),
                Codec.INT.optionalFieldOf("mastery_xp", 0).forGetter(Configuration::masteryXp)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            if (amplifierMin.isPresent() != amplifierMax.isPresent()) {
                return false;
            }
            if (amplifierMin.isPresent() && amplifierMin.get() > amplifierMax.get()) {
                return false;
            }
            if (durationMin.isPresent() != durationMax.isPresent()) {
                return false;
            }
            if (durationMin.isPresent() && durationMin.get() > durationMax.get()) {
                return false;
            }
            if (effectCountMin.isPresent() != effectCountMax.isPresent()) {
                return false;
            }
            if (effectCountMin.isPresent()) {
                int ecMin = effectCountMin.get();
                int ecMax = effectCountMax.get();
                if (ecMin < 1 || ecMin > ecMax) {
                    return false;
                }
            }
            if (masteryXp < 0) {
                return false;
            }
            return true;
        }
    }
}
