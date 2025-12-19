package dev.muon.otherworldorigins.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.network.packet.client.SyncAptitudeCapabilityCP;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.otherworld.leveling.LevelSyncHandler;
import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class InnateAptitudeBonusPower extends Power {
    private final Map<String, Integer> aptitudeBonuses;

    public InnateAptitudeBonusPower(PowerType<?> type, LivingEntity entity, Map<String, Integer> aptitudeBonuses) {
        super(type, entity);
        this.aptitudeBonuses = aptitudeBonuses;
    }

    @Override
    public void onAdded() {
        if (entity instanceof Player player) {
            applyBonuses(player);
        }
    }

    @Override
    public void onRemoved() {
        if (entity instanceof Player player) {
            removeBonuses(player);
        }
    }

    public static int getBonus(Entity entity, String aptitudeName) {
        return PowerHolderComponent.getPowers(entity, InnateAptitudeBonusPower.class).stream()
                .mapToInt(power -> power.aptitudeBonuses.getOrDefault(aptitudeName, 0))
                .sum();
    }

    private void applyBonuses(Player player) {
        AptitudeCapability cap = AptitudeCapability.get(player);
        if (cap != null && (player instanceof ServerPlayer serverPlayer)) {
            aptitudeBonuses.forEach((aptitudeName, bonus) -> {
                Aptitude aptitude = RegistryAptitudes.getAptitude(aptitudeName);
                if (aptitude != null) {
                    int currentLevel = cap.getAptitudeLevel(aptitude);
                    cap.setAptitudeLevel(aptitude, currentLevel + bonus);
                }
            });

            SyncAptitudeCapabilityCP.send(serverPlayer);
            int newPlayerLevel = LevelingUtils.getPlayerLevel(player);
            LevelSyncHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new LevelSyncHandler.SyncPlayerLevelPacket(player.getUUID(), newPlayerLevel)
            );
        }
    }

    private void removeBonuses(Player player) {
        AptitudeCapability cap = AptitudeCapability.get(player);
        if (cap != null && player instanceof ServerPlayer serverPlayer) {
            aptitudeBonuses.forEach((aptitudeName, bonus) -> {
                Aptitude aptitude = RegistryAptitudes.getAptitude(aptitudeName);
                if (aptitude != null) {
                    int currentLevel = cap.getAptitudeLevel(aptitude);
                    int revertedLevel = Math.max(currentLevel - bonus, 1);
                    cap.setAptitudeLevel(aptitude, revertedLevel);
                } else {
                    OtherworldOrigins.LOGGER.warn("Aptitude not found: " + aptitudeName);
                }
            });

            SyncAptitudeCapabilityCP.send(serverPlayer);
            int newPlayerLevel = LevelingUtils.getPlayerLevel(player);
            LevelSyncHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new LevelSyncHandler.SyncPlayerLevelPacket(player.getUUID(), newPlayerLevel)
            );
        } else {
            OtherworldOrigins.LOGGER.warn("AptitudeCapability not found for player: " + player.getName().getString());
        }
    }

    @SuppressWarnings("unchecked")
    private static final SerializableDataType<Map<String, Integer>> STRING_INT_MAP = new SerializableDataType<>(
            (Class<Map<String, Integer>>) (Class<?>) Map.class,
            (buf, map) -> {
                buf.writeInt(map.size());
                map.forEach((key, value) -> {
                    buf.writeUtf(key);
                    buf.writeInt(value);
                });
            },
            buf -> {
                int size = buf.readInt();
                Map<String, Integer> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    String key = buf.readUtf(32767);
                    int value = buf.readInt();
                    map.put(key, value);
                }
                return map;
            },
            json -> {
                if (!json.isJsonObject()) {
                    throw new com.google.gson.JsonParseException("Expected a JSON object for map");
                }
                JsonObject jsonObject = json.getAsJsonObject();
                Map<String, Integer> map = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    String key = entry.getKey();
                    int value = GsonHelper.getAsInt(jsonObject, key);
                    map.put(key, value);
                }
                return map;
            }
    );

    public static PowerFactory<?> createFactory() {
        return new PowerFactory<>(
                OtherworldOrigins.loc("innate_aptitude_bonus"),
                new SerializableData()
                        .add("aptitude_bonuses", STRING_INT_MAP),
                data -> {
                    Map<String, Integer> bonuses = data.get("aptitude_bonuses");
                    if (bonuses == null || bonuses.isEmpty()) {
                        throw new IllegalArgumentException("aptitude_bonuses cannot be empty");
                    }
                    return (type, entity) -> new InnateAptitudeBonusPower(
                            type,
                            entity,
                            bonuses
                    );
                }
        ).allowCondition();
    }
}
