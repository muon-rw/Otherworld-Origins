package dev.muon.otherworldorigins.power;

import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworld.leveling.event.AptitudeChangedEvent;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "otherworldorigins")
public class LeveledAttributePower extends Power {
    private static final UUID MODIFIER_UUID = UUID.fromString("d7184d84-7a7e-4eae-8e1a-8c5f7c2e7e3a");

    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final double valuePerLevel;
    private final double startingValue;
    private final boolean updateHealth;

    public LeveledAttributePower(PowerType<?> type, LivingEntity entity,
                                 Attribute attribute,
                                 AttributeModifier.Operation operation,
                                 double valuePerLevel,
                                 double startingValue,
                                 boolean updateHealth) {
        super(type, entity);
        this.attribute = attribute;
        this.operation = operation;
        this.valuePerLevel = valuePerLevel;
        this.startingValue = startingValue;
        this.updateHealth = updateHealth;
    }

    @Override
    public void onAdded() {
        if (entity instanceof Player player) {
            applyModifier(player);
        }
    }

    @Override
    public void onRemoved() {
        if (entity instanceof Player player) {
            removeModifier(player);
        }
    }

    public void applyModifier(Player player) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);
        if (attributeInstance != null) {
            removeModifier(player); // Remove existing modifier if any
            int level = LevelingUtils.getPlayerLevel(player);
            double value = startingValue + (level * valuePerLevel);
            AttributeModifier modifier = new AttributeModifier(
                    MODIFIER_UUID,
                    "Leveled Attribute Bonus",
                    value,
                    operation
            );
            attributeInstance.addTransientModifier(modifier);
            updateHealth(player, updateHealth);
        }
    }

    private void removeModifier(Player player) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(MODIFIER_UUID);
            updateHealth(player, updateHealth);
        }
    }

    private void updateHealth(LivingEntity entity, boolean updateHealth) {
        if (updateHealth) {
            float previousMaxHealth = entity.getMaxHealth();
            float previousHealthPercent = entity.getHealth() / previousMaxHealth;
            float afterMaxHealth = entity.getMaxHealth();
            if (afterMaxHealth != previousMaxHealth) {
                entity.setHealth(afterMaxHealth * previousHealthPercent);
            }
        }
    }

    @SubscribeEvent
    public static void onAptitudeChanged(AptitudeChangedEvent event) {
        if (event.getNewLevel() > event.getOldLevel()) {
            Entity entity = event.getPlayer();
            if (entity instanceof Player player) {
                PowerHolderComponent.getPowers(player, LeveledAttributePower.class).forEach(power -> {
                    power.applyModifier(player);
                });
            }
        }
    }

    public static PowerFactory<?> createFactory() {
        return new PowerFactory<>(
                OtherworldOrigins.loc("leveled_attribute"),
                new SerializableData()
                        .add("attribute", SerializableDataTypes.ATTRIBUTE)
                        .add("operation", SerializableDataTypes.MODIFIER_OPERATION)
                        .add("value_per_level", SerializableDataTypes.DOUBLE)
                        .add("starting_value", SerializableDataTypes.DOUBLE)
                        .add("update_health", SerializableDataTypes.BOOLEAN, true),
                data -> (type, entity) -> new LeveledAttributePower(
                        type,
                        entity,
                        data.get("attribute"),
                        data.get("operation"),
                        data.getDouble("value_per_level"),
                        data.getDouble("starting_value"),
                        data.getBoolean("update_health")
                )
        ).allowCondition();
    }
}
