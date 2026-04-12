package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

/**
 * Blocks application of mob effects whose {@link MobEffect#getCategory()} matches enabled flags.
 * Datapack shape mirrors {@code modify_status_effect_category} (without {@code amount}); aggregation
 * matches {@link io.github.edwinmindcraft.apoli.common.power.EffectImmunityPower}.
 */
public class EffectCategoryImmunityPower extends PowerFactory<EffectCategoryImmunityPower.Configuration> {

    public EffectCategoryImmunityPower() {
        super(Configuration.CODEC);
    }

    public static boolean isImmune(Entity entity, MobEffectInstance effect) {
        EffectCategoryImmunityPower factory = (EffectCategoryImmunityPower) ModPowers.EFFECT_CATEGORY_IMMUNITY.get();
        return IPowerContainer.getPowers(entity, factory).stream()
                .anyMatch(holder -> factory.isImmune((ConfiguredPower<Configuration, ?>) holder.value(), effect));
    }

    public boolean isImmune(ConfiguredPower<Configuration, ?> configuration, MobEffectInstance effect) {
        return isImmune(configuration, effect.getEffect());
    }

    public boolean isImmune(ConfiguredPower<Configuration, ?> configuration, MobEffect effect) {
        Configuration config = configuration.getConfiguration();
        MobEffectCategory category = effect.getCategory();
        return (config.affectBeneficial() && category == MobEffectCategory.BENEFICIAL)
                || (config.affectHarmful() && category == MobEffectCategory.HARMFUL)
                || (config.affectNeutral() && category == MobEffectCategory.NEUTRAL);
    }

    public record Configuration(
            boolean affectBeneficial,
            boolean affectHarmful,
            boolean affectNeutral
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("affect_beneficial").forGetter(Configuration::affectBeneficial),
                Codec.BOOL.fieldOf("affect_harmful").forGetter(Configuration::affectHarmful),
                Codec.BOOL.fieldOf("affect_neutral").forGetter(Configuration::affectNeutral)
        ).apply(instance, Configuration::new));
    }
}
