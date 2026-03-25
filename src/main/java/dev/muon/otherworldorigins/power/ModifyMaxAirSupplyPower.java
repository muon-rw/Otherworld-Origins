package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;

/**
 * Increases {@link net.minecraft.world.entity.Entity#getMaxAirSupply} while active (mixin on Entity).
 */
public class ModifyMaxAirSupplyPower extends PowerFactory<ModifyMaxAirSupplyPower.Configuration> {
    public ModifyMaxAirSupplyPower() {
        super(Configuration.CODEC);
    }

    public record Configuration(int bonus) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("bonus").forGetter(Configuration::bonus)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return bonus >= 0;
        }
    }
}
