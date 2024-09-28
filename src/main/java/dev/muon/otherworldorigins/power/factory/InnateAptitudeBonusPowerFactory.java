package dev.muon.otherworldorigins.power.factory;

import dev.muon.otherworldorigins.power.configuration.InnateAptitudeBonusConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class InnateAptitudeBonusPowerFactory extends PowerFactory<InnateAptitudeBonusConfiguration> {
    public InnateAptitudeBonusPowerFactory() {
        super(InnateAptitudeBonusConfiguration.CODEC);
    }

    @Override
    public void onAdded(InnateAptitudeBonusConfiguration configuration, Entity entity) {
        if (entity instanceof Player player) {
            configuration.applyBonuses(player);
        }
    }

    @Override
    public void onRemoved(InnateAptitudeBonusConfiguration configuration, Entity entity) {
        if (entity instanceof Player player) {
            configuration.removeBonuses(player);
        }
    }
}