package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Stamps potions brewed by the holder with bonus duration/amplifier per effect category.
 * Applied at brewing-stand brew completion via {@code BrewingStandBlockEntityMixin}.
 */
public final class ModifyBrewedPotionPower extends PowerType<ModifyBrewedPotionPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("affect_beneficial", true).forGetter(Configuration::affectBeneficial),
            Codec.BOOL.optionalFieldOf("affect_harmful", true).forGetter(Configuration::affectHarmful),
            Codec.BOOL.optionalFieldOf("affect_neutral", true).forGetter(Configuration::affectNeutral),
            Codec.FLOAT.optionalFieldOf("duration_multiplier", 1.0f).forGetter(Configuration::durationMultiplier),
            Codec.INT.optionalFieldOf("amplifier_modifier", 0).forGetter(Configuration::amplifierModifier)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static boolean appliesTo(Configuration config, MobEffectCategory category) {
        return (config.affectBeneficial() && category == MobEffectCategory.BENEFICIAL)
                || (config.affectHarmful() && category == MobEffectCategory.HARMFUL)
                || (config.affectNeutral() && category == MobEffectCategory.NEUTRAL);
    }

    /**
     * Every active {@code modify_brewed_potion} configuration the entity holds. Respects
     * suppression and the power's JSON-level condition. Empty if none are active.
     */
    public static List<Configuration> getActiveConfigs(LivingEntity entity) {
        return PowerLookup.active(entity, ModPowers.MODIFY_BREWED_POTION, Configuration.class);
    }

    public record Configuration(
            boolean affectBeneficial,
            boolean affectHarmful,
            boolean affectNeutral,
            float durationMultiplier,
            int amplifierModifier
    ) {
    }
}
