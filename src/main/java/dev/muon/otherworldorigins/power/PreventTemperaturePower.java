package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureImmunityEnum;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil;

import java.util.Optional;


public class PreventTemperaturePower extends PowerFactory<PreventTemperaturePower.Configuration> {
    public PreventTemperaturePower() {
        super(PreventTemperaturePower.Configuration.CODEC);
    }


    @Override
    public void onGained(Configuration configuration, Entity entity) {
        if (entity instanceof Player player) {
            applyImmunity(player, configuration.tempType, true);
        }
    }

    @Override
    public void onLost(Configuration configuration, Entity entity) {
        if (entity instanceof Player player) {
            applyImmunity(player, configuration.tempType, false);
        }
    }

    private void applyImmunity(Player player, Optional<String> tempType, boolean add) {
        tempType.ifPresentOrElse(
                type -> applyImmunity(player, TemperatureImmunityEnum.valueOf(type.toUpperCase()), add),
                () -> {
                    applyImmunity(player, TemperatureImmunityEnum.HIGH_ALTITUDE, add);
                    applyImmunity(player, TemperatureImmunityEnum.ON_FIRE, add);
                }
        );
    }

    private void applyImmunity(Player player, TemperatureImmunityEnum immunity, boolean add) {
        if (add) {
            TemperatureUtil.addImmunity(player, immunity);
        } else {
            TemperatureUtil.removeImmunity(player, immunity);
        }
    }

    public record Configuration(Optional<String> tempType) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("temp_type").forGetter(Configuration::tempType)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return tempType.isEmpty() || isValidTempType(tempType.get());
        }

        private boolean isValidTempType(String type) {
            try {
                TemperatureImmunityEnum.valueOf(type.toUpperCase());
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}
