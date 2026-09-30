package dev.muon.raven_dnd_origins.kit;

import com.google.gson.JsonObject;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import dev.muon.raven_core.leveling.LevelingUtils;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.EquipmentChangedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a kit's sections in dependency order: origins are cleared first so their innate aptitude bonuses come off,
 * base aptitudes go in so level-gated layers accept their choices, then origins, passives and skills, and items last
 * so origin starter kits are replaced. Sections a kit leaves out are left as they are.
 */
@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public final class KitApplier {
    // Equipment attributes, max health and max mana among them, apply on the player's next tick
    private static final int REFRESH_DELAY_TICKS = 2;
    private static final Map<UUID, Integer> PENDING_REFRESH = new ConcurrentHashMap<>();

    private KitApplier() {}

    static List<String> apply(ServerPlayer player, JsonObject kit) {
        List<String> problems = new ArrayList<>();
        AptitudeCapability capability = AptitudeCapability.get(player);
        boolean origins = kit.has("origins");
        if (origins) {
            KitOrigins.clear(player);
        }
        if (capability == null) {
            if (kit.has("aptitudes") || kit.has("passives") || kit.has("skills")) {
                problems.add("player has no Just Leveling data");
            }
        } else if (kit.has("aptitudes")) {
            KitLeveling.reset(capability);
            KitLeveling.setAptitudes(player, capability, kit.getAsJsonObject("aptitudes"), problems);
        }
        if (origins) {
            KitOrigins.choose(player, kit.getAsJsonObject("origins"), problems);
        }
        if (capability != null) {
            if (kit.has("passives")) {
                KitLeveling.setPassives(capability, kit.get("passives"), problems);
            }
            if (kit.has("skills")) {
                KitLeveling.setSkills(player, capability, kit.get("skills"), problems);
            }
            KitLeveling.sync(player);
        }
        if (KitItems.present(kit)) {
            KitItems.clear(player);
            KitItems.give(player, kit, problems);
            KitSpells.learnContained(player);
        }
        PENDING_REFRESH.put(player.getUUID(), REFRESH_DELAY_TICKS);
        return problems;
    }

    static JsonObject capture(ServerPlayer player) {
        JsonObject kit = new JsonObject();
        kit.addProperty("description", "Saved from " + player.getGameProfile().getName() + " at level " + LevelingUtils.getPlayerLevel(player));
        kit.add("origins", KitOrigins.capture(player));
        AptitudeCapability capability = AptitudeCapability.get(player);
        if (capability != null) {
            kit.add("aptitudes", KitLeveling.captureAptitudes(player, capability));
            kit.add("passives", KitLeveling.capturePassives(capability));
            kit.add("skills", KitLeveling.captureSkills(capability));
        }
        KitItems.capture(player, kit);
        return kit;
    }

    @SubscribeEvent
    public static void refreshOnceEquipmentSettles(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Integer remaining = PENDING_REFRESH.get(player.getUUID());
        if (remaining == null) {
            return;
        }
        if (remaining > 0) {
            PENDING_REFRESH.put(player.getUUID(), remaining - 1);
            return;
        }
        PENDING_REFRESH.remove(player.getUUID());
        refresh(player);
    }

    private static void refresh(ServerPlayer player) {
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        MagicData magic = MagicData.getPlayerMagicData(player);
        magic.setMana((float) player.getAttributeValue(AttributeRegistry.MAX_MANA));
        magic.getPlayerCooldowns().clearCooldowns();
        magic.getPlayerCooldowns().syncToPlayer(player);
        // Accessories syncs curio stacks after Iron's reacts to the change, so the client built its spell list from the old book
        PacketDistributor.sendToPlayer(player, new EquipmentChangedPacket());
    }
}
