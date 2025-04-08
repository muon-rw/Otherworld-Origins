package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworld.leveling.event.AptitudeChangedEvent;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
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
public class LeveledAttributePower extends PowerFactory<LeveledAttributePower.Configuration> {
    private static final UUID MODIFIER_UUID = UUID.fromString("d7184d84-7a7e-4eae-8e1a-8c5f7c2e7e3a");

    public LeveledAttributePower() {
        super(Configuration.CODEC);
    }

    @Override
    public void onAdded(Configuration configuration, Entity entity) {
        if (entity instanceof Player player) {
            applyModifier(configuration, player);
        }
    }

    @Override

    public void onRemoved(Configuration configuration, Entity entity) {
        if (entity instanceof Player player) {
            removeModifier(configuration, player);
        }
    }

    private void applyModifier(Configuration configuration, Player player) {
        AttributeInstance attributeInstance = player.getAttribute(configuration.attribute);
        if (attributeInstance != null) {
            removeModifier(configuration, player); // Remove existing modifier if any
            int level = LevelingUtils.getPlayerLevel(player);
            double value = configuration.startingValue + (level * configuration.valuePerLevel);
            AttributeModifier modifier = new AttributeModifier(
                    MODIFIER_UUID,
                    "Leveled Attribute Bonus",
                    value,
                    configuration.operation
            );
            attributeInstance.addTransientModifier(modifier);
            updateHealth(player, configuration.updateHealth);
        }
    }

    private void removeModifier(Configuration configuration, Player player) {
        AttributeInstance attributeInstance = player.getAttribute(configuration.attribute);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(MODIFIER_UUID);
            updateHealth(player, configuration.updateHealth);
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
                IPowerContainer.get(player).ifPresent(container -> {
                    container.getPowers(ModPowers.LEVELED_ATTRIBUTE.get()).forEach(powerHolder -> {
                        Configuration config = powerHolder.value().getConfiguration();
                        ((LeveledAttributePower) ModPowers.LEVELED_ATTRIBUTE.get()).applyModifier(config, player);
                    });
                });
            }
        }
    }

    public record Configuration(
            Attribute attribute,
            AttributeModifier.Operation operation,
            double valuePerLevel,
            double startingValue,
            boolean updateHealth
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                io.github.apace100.calio.data.SerializableDataTypes.ATTRIBUTE.fieldOf("attribute").forGetter(Configuration::attribute),
                io.github.apace100.calio.data.SerializableDataTypes.MODIFIER_OPERATION.fieldOf("operation").forGetter(Configuration::operation),
                Codec.DOUBLE.fieldOf("value_per_level").forGetter(Configuration::valuePerLevel),
                Codec.DOUBLE.fieldOf("starting_value").forGetter(Configuration::startingValue),
                CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "update_health", true).forGetter(Configuration::updateHealth)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}