package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.network.packet.client.SyncAptitudeCapabilityCP;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.Map;
import java.util.Set;

public class InnateAptitudeBonusPower extends PowerType<InnateAptitudeBonusPower.Cfg> {

    public record Cfg(Map<String, Integer> aptitudeBonuses) {}

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("aptitude_bonuses").forGetter(Cfg::aptitudeBonuses)
    ).apply(instance, Cfg::new));

    @Override
    public MapCodec<Cfg> configCodec() {
        return CODEC;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.rawOwner() instanceof Player player) {
            applyBonuses(player, cfg.aptitudeBonuses());
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Cfg cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        if (holder.rawOwner() instanceof Player player) {
            removeBonuses(player, cfg.aptitudeBonuses());
        }
    }

    public static int getBonus(Entity entity, String aptitudeName) {
        int sum = 0;
        for (Cfg cfg : configsOn(entity)) {
            Integer bonus = cfg.aptitudeBonuses().get(aptitudeName);
            if (bonus != null) sum += bonus;
        }
        return sum;
    }

    /**
     * Sums bonuses across all INNATE_APTITUDE_BONUS powers for any aptitude in
     * {@code aptitudeNames}. Equivalent to calling {@link #getBonus} for each name and
     * adding the results, but iterates the power list once instead of N times.
     */
    public static int sumBonusesForAptitudes(Entity entity, Set<String> aptitudeNames) {
        if (aptitudeNames.isEmpty()) return 0;
        int sum = 0;
        for (Cfg cfg : configsOn(entity)) {
            for (Map.Entry<String, Integer> entry : cfg.aptitudeBonuses().entrySet()) {
                if (aptitudeNames.contains(entry.getKey())) sum += entry.getValue();
            }
        }
        return sum;
    }

    // Deliberately does not evaluate power conditions: this feeds LevelingUtils.getPlayerLevel,
    // which datapack conditions may themselves consult.
    private static java.util.List<Cfg> configsOn(Entity entity) {
        if (entity == null) return java.util.List.of();
        PowerContainer container = PowerContainer.of(entity);
        if (container == null) return java.util.List.of();
        java.util.List<ResourceLocation> powers = container.powersOfType(ModPowers.INNATE_APTITUDE_BONUS);
        if (powers.isEmpty()) return java.util.List.of();
        java.util.List<Cfg> out = new java.util.ArrayList<>(powers.size());
        for (ResourceLocation powerId : powers) {
            if (container.isSuppressed(powerId)) continue;
            Power power = ApoliPowers.get(powerId);
            if (power != null && power.config() instanceof Cfg cfg) out.add(cfg);
        }
        return out;
    }

    private void applyBonuses(Player player, Map<String, Integer> aptitudeBonuses) {
        AptitudeCapability cap = AptitudeCapability.get(player);
        if (cap == null || !(player instanceof ServerPlayer serverPlayer)) {
            RavenDndOrigins.LOGGER.warn("AptitudeCapability not found for player: {}", player.getName().getString());
            return;
        }
        aptitudeBonuses.forEach((aptitudeName, bonus) -> {
            Aptitude aptitude = RegistryAptitudes.getAptitude(aptitudeName);
            if (aptitude != null) {
                // Baseline shift, not a level-up: writes the map directly so no AptitudeChangedEvent fires.
                cap.aptitudeLevel.put(aptitude.getName(), cap.getAptitudeLevel(aptitude) + bonus);
            } else {
                RavenDndOrigins.LOGGER.warn("Aptitude not found: {}", aptitudeName);
            }
        });
        syncLevel(serverPlayer);
    }

    private void removeBonuses(Player player, Map<String, Integer> aptitudeBonuses) {
        AptitudeCapability cap = AptitudeCapability.get(player);
        if (cap == null || !(player instanceof ServerPlayer serverPlayer)) {
            RavenDndOrigins.LOGGER.warn("AptitudeCapability not found for player: {}", player.getName().getString());
            return;
        }
        aptitudeBonuses.forEach((aptitudeName, bonus) -> {
            Aptitude aptitude = RegistryAptitudes.getAptitude(aptitudeName);
            if (aptitude != null) {
                cap.aptitudeLevel.put(aptitude.getName(), Math.max(cap.getAptitudeLevel(aptitude) - bonus, 1));
            } else {
                RavenDndOrigins.LOGGER.warn("Aptitude not found: {}", aptitudeName);
            }
        });
        syncLevel(serverPlayer);
    }

    private static void syncLevel(ServerPlayer player) {
        SyncAptitudeCapabilityCP.send(player);
        if (ModList.get().isLoaded("dynamic_difficulty")) {
            dev.muon.dynamic_difficulty.api.PlayerLevelProvider.requestPlayerLevelUpdate(player);
        }
    }
}
