package dev.muon.otherworldorigins.attribute;

import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworld.leveling.event.AptitudeChangedEvent;
import dev.muon.otherworld.leveling.event.PassiveChangedEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "otherworldorigins")
public class HealthPerLevelHandler {
    private static final UUID HEALTH_PER_LEVEL_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final Map<Player, Integer> lastKnownLevels = new HashMap<>();
    private static final Map<Player, Double> lastKnownAttributeValues = new HashMap<>();

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
        Player player = event.getEntity();
        lastKnownLevels.remove(player);
        lastKnownAttributeValues.remove(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            // Check every 20 ticks (1 second) to catch attribute changes from passive system
            if (player.tickCount % 20 == 0) {
                AttributeInstance healthPerLevelAttr = player.getAttribute(ModAttributes.HEALTH_PER_LEVEL.get());
                if (healthPerLevelAttr != null) {
                    int currentLevel = LevelingUtils.getPlayerLevel(player);
                    double currentAttributeValue = healthPerLevelAttr.getValue();
                    
                    Integer lastLevel = lastKnownLevels.get(player);
                    Double lastAttributeValue = lastKnownAttributeValues.get(player);
                    
                    if (lastLevel == null || lastLevel != currentLevel || 
                        lastAttributeValue == null || lastAttributeValue != currentAttributeValue) {
                        updateHealthModifier(player);
                    }
                }
            }
        }
    }

    public static void updateHealthModifier(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        AttributeInstance healthPerLevelAttr = player.getAttribute(ModAttributes.HEALTH_PER_LEVEL.get());
        AttributeInstance maxHealthAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);

        if (healthPerLevelAttr == null || maxHealthAttr == null) {
            return;
        }

        // Store previous health state for proportional healing
        float previousMaxHealth = player.getMaxHealth();
        float previousHealth = player.getHealth();
        float missingHealth = previousMaxHealth - previousHealth;

        // Remove existing modifier
        maxHealthAttr.removeModifier(HEALTH_PER_LEVEL_MODIFIER_UUID);

        // Calculate new modifier value
        int characterLevel = LevelingUtils.getPlayerLevel(player);
        double healthPerLevelValue = healthPerLevelAttr.getValue();
        double modifierValue = characterLevel * healthPerLevelValue;

        // Update tracking maps
        lastKnownLevels.put(player, characterLevel);
        lastKnownAttributeValues.put(player, healthPerLevelValue);

        if (modifierValue > 0) {
            // Apply new modifier
            AttributeModifier modifier = new AttributeModifier(
                    HEALTH_PER_LEVEL_MODIFIER_UUID,
                    "Health per Character Level",
                    modifierValue,
                    AttributeModifier.Operation.ADDITION
            );
            maxHealthAttr.addPermanentModifier(modifier);

            // Proportionally heal the player: maintain equivalent missing health
            // Example: 5/8 health (missing 3) -> add 3 max -> 8/11 health (still missing 3)
            float newMaxHealth = player.getMaxHealth();
            if (newMaxHealth != previousMaxHealth && newMaxHealth > 0) {
                float newHealth = Math.max(previousHealth, newMaxHealth - missingHealth);
                player.setHealth(newHealth);
            }
        }
    }
}
