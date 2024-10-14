package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;

public class ModifyThirstExhaustionPower extends PowerFactory<ModifyThirstExhaustionPower.Configuration> {
    public ModifyThirstExhaustionPower() {
        super(ModifyThirstExhaustionPower.Configuration.CODEC);
    }


    public record Configuration(float amount) implements IDynamicFeatureConfiguration {
        public static final Codec<ModifyThirstExhaustionPower.Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("amount").forGetter(ModifyThirstExhaustionPower.Configuration::amount)
        ).apply(instance, ModifyThirstExhaustionPower.Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}