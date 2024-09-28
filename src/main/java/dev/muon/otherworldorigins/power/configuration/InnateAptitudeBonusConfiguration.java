package dev.muon.otherworldorigins.power.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import dev.muon.otherworldorigins.power.ModPowers;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;

import java.util.Map;

public record InnateAptitudeBonusConfiguration(Map<String, Integer> aptitudeBonuses) implements IDynamicFeatureConfiguration {
    public static final Codec<InnateAptitudeBonusConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("aptitude_bonuses").forGetter(InnateAptitudeBonusConfiguration::aptitudeBonuses)
    ).apply(instance, InnateAptitudeBonusConfiguration::new));

    @Override
    public boolean isConfigurationValid() {
        return !aptitudeBonuses.isEmpty();
    }

    public static boolean hasPower(Entity entity) {
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(entity);
        return powerContainer != null && powerContainer.hasPower(ModPowers.INNATE_APTITUDE_BONUS.get());
    }

    public static int getBonus(Entity entity, String aptitudeName) {
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(entity);
        if (powerContainer != null) {
            var playerPowers = powerContainer.getPowers(ModPowers.INNATE_APTITUDE_BONUS.get());
            return playerPowers.stream()
                    .map(holder -> holder.value().getConfiguration())
                    .mapToInt(config -> config.aptitudeBonuses().getOrDefault(aptitudeName, 0))
                    .sum();
        }
        return 0;
    }

    public void applyBonuses(Player player) {
        AptitudeCapability cap = AptitudeCapability.get(player);
        if (cap != null) {
            aptitudeBonuses.forEach((aptitudeName, bonus) -> {
                Aptitude aptitude = RegistryAptitudes.getAptitude(aptitudeName);
                if (aptitude != null) {
                    cap.addAptitudeLevel(aptitude, bonus);
                }
            });
        }
    }

    public void removeBonuses(Player player) {
        AptitudeCapability cap = AptitudeCapability.get(player);
        if (cap != null) {
            aptitudeBonuses.forEach((aptitudeName, bonus) -> {
                Aptitude aptitude = RegistryAptitudes.getAptitude(aptitudeName);
                if (aptitude != null) {
                    cap.addAptitudeLevel(aptitude, -bonus);
                }
            });
        }
    }
}