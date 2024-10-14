package dev.muon.otherworldorigins.power;

import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureImmunityEnum;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil;


public class PreventTemperaturePower extends PowerFactory<NoConfiguration> {
    public PreventTemperaturePower() {
        super(NoConfiguration.CODEC);
    }

    @Override
    public void onGained(NoConfiguration configuration, Entity entity) {
        if (entity instanceof Player player) {
            TemperatureUtil.addImmunity(player, TemperatureImmunityEnum.HIGH_ALTITUDE);
            TemperatureUtil.addImmunity(player, TemperatureImmunityEnum.ON_FIRE);
        }
    }

    @Override
    public void onLost(NoConfiguration configuration, Entity entity) {
        if (entity instanceof Player player) {
            TemperatureUtil.removeImmunity(player, TemperatureImmunityEnum.HIGH_ALTITUDE);
            TemperatureUtil.removeImmunity(player, TemperatureImmunityEnum.ON_FIRE);
        }
    }
}
