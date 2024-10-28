package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;

public class GoldDurabilityPower extends PowerFactory<GoldDurabilityPower.Configuration> {
    public GoldDurabilityPower() {
        super(Configuration.CODEC);
    }

    public record Configuration(int amount) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("amount").forGetter(Configuration::amount)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return amount >= 0;
        }
    }
}