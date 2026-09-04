package dev.muon.raven_dnd_origins.attribute;

import dev.muon.raven_core.leveling.LevelingUtils;
import dev.muon.raven_core.leveling.event.AptitudeChangedEvent;
import dev.muon.raven_core.leveling.event.PassiveChangedEvent;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class HealthPerLevelHandler {
    private static final ResourceLocation HEALTH_PER_LEVEL_MODIFIER = RavenDndOrigins.loc("health_per_character_level");
    private static final Map<UUID, Integer> lastKnownLevels = new HashMap<>();
    private static final Map<UUID, Double> lastKnownAttributeValues = new HashMap<>();

    @SubscribeEvent
    public static void onAptitudeChanged(AptitudeChangedEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            updateHealthModifier(player);
        }
    }

    @SubscribeEvent
    public static void onPassiveChanged(PassiveChangedEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            updateHealthModifier(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updateHealthModifier(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            updateHealthModifier(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        lastKnownLevels.remove(id);
        lastKnownAttributeValues.remove(id);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        AttributeInstance healthPerLevelAttr = player.getAttribute(ModAttributes.HEALTH_PER_LEVEL);
        if (healthPerLevelAttr == null) {
            return;
        }
        int currentLevel = LevelingUtils.getPlayerLevel(player);
        double currentAttributeValue = healthPerLevelAttr.getValue();

        Integer lastLevel = lastKnownLevels.get(player.getUUID());
        Double lastAttributeValue = lastKnownAttributeValues.get(player.getUUID());

        if (lastLevel == null || lastLevel != currentLevel
                || lastAttributeValue == null || lastAttributeValue != currentAttributeValue) {
            updateHealthModifier(player);
        }
    }

    public static void updateHealthModifier(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        AttributeInstance healthPerLevelAttr = player.getAttribute(ModAttributes.HEALTH_PER_LEVEL);
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);

        if (healthPerLevelAttr == null || maxHealthAttr == null) {
            return;
        }

        float previousMaxHealth = player.getMaxHealth();
        // Clamp percent to [0,1]: if a prior step left health > maxHealth (vanilla does not
        // auto-clamp on removeModifier), an unclamped >1.0 percent would chain into the next
        // setHealth-based handler and cap the player at the new (smaller) max, dropping HP.
        float previousHealthPercent = previousMaxHealth > 0
                ? Mth.clamp(player.getHealth() / previousMaxHealth, 0.0f, 1.0f)
                : 1.0f;

        maxHealthAttr.removeModifier(HEALTH_PER_LEVEL_MODIFIER);

        int characterLevel = LevelingUtils.getPlayerLevel(player);
        double healthPerLevelValue = healthPerLevelAttr.getValue();
        double modifierValue = characterLevel * healthPerLevelValue;

        lastKnownLevels.put(player.getUUID(), characterLevel);
        lastKnownAttributeValues.put(player.getUUID(), healthPerLevelValue);

        if (modifierValue > 0) {
            // Transient modifier: auto-cleaned on death/relog, preventing orphans.
            maxHealthAttr.addTransientModifier(new AttributeModifier(
                    HEALTH_PER_LEVEL_MODIFIER, modifierValue, AttributeModifier.Operation.ADD_VALUE));
        }

        // Always reconcile health when max changed, even when no new modifier was added
        // (e.g., modifier value dropped to 0). Without this, the player's stored health
        // can exceed the new max, and the next handler that captures previousHealthPercent
        // would see >1.0 and clamp the player to a much lower absolute HP.
        float newMaxHealth = player.getMaxHealth();
        if (newMaxHealth != previousMaxHealth && newMaxHealth > 0) {
            player.setHealth(newMaxHealth * previousHealthPercent);
        }
    }
}
