package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

/**
 * Blocks application of mob effects whose {@link MobEffect#getCategory()} matches enabled flags.
 * Datapack shape mirrors {@code modify_status_effect_category} (without {@code amount}).
 */
public final class EffectCategoryImmunityPower extends PowerType<EffectCategoryImmunityPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("affect_beneficial").forGetter(Configuration::affectBeneficial),
            Codec.BOOL.fieldOf("affect_harmful").forGetter(Configuration::affectHarmful),
            Codec.BOOL.fieldOf("affect_neutral").forGetter(Configuration::affectNeutral)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static boolean isImmune(Entity entity, MobEffectInstance effect) {
        MobEffectCategory category = effect.getEffect().value().getCategory();
        boolean[] immune = new boolean[]{false};
        PowerLookup.forEach(entity, ModPowers.EFFECT_CATEGORY_IMMUNITY, Configuration.class, cfg -> {
            if (immune[0]) return;
            if (matches(cfg, category)) immune[0] = true;
        });
        return immune[0];
    }

    private static boolean matches(Configuration config, MobEffectCategory category) {
        return (config.affectBeneficial() && category == MobEffectCategory.BENEFICIAL)
                || (config.affectHarmful() && category == MobEffectCategory.HARMFUL)
                || (config.affectNeutral() && category == MobEffectCategory.NEUTRAL);
    }

    public record Configuration(
            boolean affectBeneficial,
            boolean affectHarmful,
            boolean affectNeutral
    ) {
    }
}
