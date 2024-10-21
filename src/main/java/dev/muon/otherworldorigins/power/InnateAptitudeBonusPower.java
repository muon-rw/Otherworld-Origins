package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.medieval.Medieval;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public class InnateAptitudeBonusPower extends PowerFactory<InnateAptitudeBonusPower.Configuration> {
    public InnateAptitudeBonusPower() {
        super(Configuration.CODEC);
    }

    @Override
    public void onGained(Configuration configuration, Entity entity) {
        if (entity instanceof Player player) {
            applyBonuses(player, configuration.aptitudeBonuses());
        }
    }

    @Override
    public void onLost(Configuration configuration, Entity entity) {
        if (entity instanceof Player player) {
            removeBonuses(player, configuration.aptitudeBonuses());
        }
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
        Medieval.LOGGER.debug("Could not retrieve power container from " + entity.getName() + " for " + aptitudeName);
        return 0;
    }

    private void applyBonuses(Player player, Map<String, Integer> aptitudeBonuses) {
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

    private void removeBonuses(Player player, Map<String, Integer> aptitudeBonuses) {
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

    public record Configuration(
            Map<String, Integer> aptitudeBonuses) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("aptitude_bonuses").forGetter(Configuration::aptitudeBonuses)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return !aptitudeBonuses.isEmpty();
        }
    }
}