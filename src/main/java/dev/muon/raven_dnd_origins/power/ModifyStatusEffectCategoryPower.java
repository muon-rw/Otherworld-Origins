package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class ModifyStatusEffectCategoryPower extends PowerType<ModifyStatusEffectCategoryPower.Configuration> {

    public static final ModifyStatusEffectCategoryPower INSTANCE = new ModifyStatusEffectCategoryPower();

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("affect_beneficial").forGetter(Configuration::affectBeneficial),
            Codec.BOOL.fieldOf("affect_harmful").forGetter(Configuration::affectHarmful),
            Codec.BOOL.fieldOf("affect_neutral").forGetter(Configuration::affectNeutral),
            Codec.FLOAT.fieldOf("amount").forGetter(Configuration::amount)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static boolean doesApply(Configuration config, Holder<MobEffect> effect) {
        MobEffectCategory category = effect.value().getCategory();
        return (config.affectBeneficial() && category == MobEffectCategory.BENEFICIAL)
                || (config.affectHarmful() && category == MobEffectCategory.HARMFUL)
                || (config.affectNeutral() && category == MobEffectCategory.NEUTRAL);
    }

    public static float getDurationMultiplier(LivingEntity entity, MobEffectInstance effect) {
        float multiplier = 1.0f;
        for (Configuration config : PowerLookup.active(entity, ModPowers.MODIFY_STATUS_EFFECT_CATEGORY, Configuration.class)) {
            if (doesApply(config, effect.getEffect())) {
                multiplier *= config.amount();
            }
        }
        return multiplier;
    }

    public record Configuration(
            boolean affectBeneficial,
            boolean affectHarmful,
            boolean affectNeutral,
            float amount
    ) {
    }
}
