package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ModifyStatusEffectCategoryPower extends PowerFactory<ModifyStatusEffectCategoryPower.Configuration> {

    public static final ModifyStatusEffectCategoryPower INSTANCE = new ModifyStatusEffectCategoryPower();

    private ModifyStatusEffectCategoryPower() {
        super(Configuration.CODEC);
    }

    public static boolean doesApply(Configuration config, MobEffect effect) {
        MobEffectCategory category = effect.getCategory();
        return (config.affectBeneficial && category == MobEffectCategory.BENEFICIAL) ||
                (config.affectHarmful && category == MobEffectCategory.HARMFUL) ||
                (config.affectNeutral && category == MobEffectCategory.NEUTRAL);
    }

    public record Configuration(
            boolean affectBeneficial,
            boolean affectHarmful,
            boolean affectNeutral,
            float amount
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("affect_beneficial").forGetter(Configuration::affectBeneficial),
                Codec.BOOL.fieldOf("affect_harmful").forGetter(Configuration::affectHarmful),
                Codec.BOOL.fieldOf("affect_neutral").forGetter(Configuration::affectNeutral),
                Codec.FLOAT.fieldOf("amount").forGetter(Configuration::amount)
        ).apply(instance, Configuration::new));
    }
}