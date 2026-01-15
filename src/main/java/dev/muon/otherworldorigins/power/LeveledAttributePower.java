package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworld.leveling.event.AptitudeChangedEvent;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID)
public class LeveledAttributePower extends PowerFactory<LeveledAttributePower.Configuration> {

    public LeveledAttributePower() {
        super(Configuration.CODEC);
        this.ticking(true);
    }

    @Override
    public void tick(ConfiguredPower<Configuration, ?> configuredPower, Entity entity) {
        if (configuredPower.isActive(entity)) {
            configuredPower.getConfiguration().add(entity);
        } else {
            configuredPower.getConfiguration().remove(entity);
        }
    }

    @Override
    protected int tickInterval(Configuration configuration, Entity entity) {
        return configuration.tickRate();
    }

    @Override
    protected void onRemoved(Configuration configuration, Entity entity) {
        configuration.remove(entity);
    }

    @SubscribeEvent
    public static void onAptitudeChanged(AptitudeChangedEvent event) {
        if (event.getNewLevel() > event.getOldLevel()) {
            Entity entity = event.getPlayer();
            if (entity instanceof Player player) {
                IPowerContainer.get(player).ifPresent(container -> {
                    container.getPowers(ModPowers.LEVELED_ATTRIBUTE.get()).forEach(powerHolder -> {
                        powerHolder.value().getConfiguration().update(player);
                    });
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reapplyAllModifiers(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reapplyAllModifiers(player);
        }
    }

    /**
     * Re-apply all leveled attribute modifiers for a player.
     * Called on login/respawn to ensure transient modifiers are restored immediately.
     */
    private static void reapplyAllModifiers(Player player) {
        IPowerContainer.get(player).ifPresent(container -> {
            container.getPowers(ModPowers.LEVELED_ATTRIBUTE.get()).forEach(powerHolder -> {
                powerHolder.value().getConfiguration().add(player);
            });
        });
    }

    public record Configuration(
            Attribute attribute,
            AttributeModifier.Operation operation,
            double valuePerLevel,
            double startingValue,
            boolean updateHealth,
            int tickRate,
            UUID modifierUuid  // UUID is generated at deserialization time, making it unique per power definition
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                io.github.apace100.calio.data.SerializableDataTypes.ATTRIBUTE.fieldOf("attribute").forGetter(Configuration::attribute),
                io.github.apace100.calio.data.SerializableDataTypes.MODIFIER_OPERATION.fieldOf("operation").forGetter(Configuration::operation),
                Codec.DOUBLE.fieldOf("value_per_level").forGetter(Configuration::valuePerLevel),
                Codec.DOUBLE.fieldOf("starting_value").forGetter(Configuration::startingValue),
                CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "update_health", true).forGetter(Configuration::updateHealth),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "tick_rate", 20).forGetter(Configuration::tickRate)
        ).apply(instance, (attr, op, vpl, sv, uh, tr) -> new Configuration(attr, op, vpl, sv, uh, tr, UUID.randomUUID())));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }

        /**
         * Add the leveled attribute modifier to the entity.
         * Following Apoli's pattern from AttributeConfiguration.
         */
        public void add(Entity entity) {
            if (!(entity instanceof LivingEntity living) || entity.level().isClientSide()) {
                return;
            }

            AttributeInstance attributeInstance = living.getAttribute(this.attribute);
            if (attributeInstance == null) {
                return;
            }

            // Only add if not already present
            if (attributeInstance.getModifier(this.modifierUuid) != null) {
                return;
            }

            float previousMaxHealth = living.getMaxHealth();
            float previousHealthPercent = living.getHealth() / previousMaxHealth;

            int level = living instanceof Player player ? LevelingUtils.getPlayerLevel(player) : 1;
            double value = this.startingValue + ((level - 1) * this.valuePerLevel);

            AttributeModifier modifier = new AttributeModifier(
                    this.modifierUuid,
                    "Leveled Attribute Bonus",
                    value,
                    this.operation
            );
            attributeInstance.addTransientModifier(modifier);

            float afterMaxHealth = living.getMaxHealth();
            if (this.updateHealth && afterMaxHealth != previousMaxHealth) {
                living.setHealth(afterMaxHealth * previousHealthPercent);
            }
        }

        /**
         * Remove the leveled attribute modifier from the entity.
         */
        public void remove(Entity entity) {
            if (!(entity instanceof LivingEntity living) || entity.level().isClientSide()) {
                return;
            }

            AttributeInstance attributeInstance = living.getAttribute(this.attribute);
            if (attributeInstance == null || attributeInstance.getModifier(this.modifierUuid) == null) {
                return;
            }

            float previousMaxHealth = living.getMaxHealth();
            float previousHealthPercent = living.getHealth() / previousMaxHealth;

            attributeInstance.removeModifier(this.modifierUuid);

            float afterMaxHealth = living.getMaxHealth();
            if (this.updateHealth && afterMaxHealth != previousMaxHealth) {
                living.setHealth(afterMaxHealth * previousHealthPercent);
            }
        }

        /**
         * Update the modifier value (e.g., when player level changes).
         * Removes and re-adds with the new calculated value.
         */
        public void update(Entity entity) {
            if (!(entity instanceof LivingEntity living) || entity.level().isClientSide()) {
                return;
            }

            AttributeInstance attributeInstance = living.getAttribute(this.attribute);
            if (attributeInstance == null) {
                return;
            }

            float previousMaxHealth = living.getMaxHealth();
            float previousHealthPercent = living.getHealth() / previousMaxHealth;

            // Remove existing modifier
            attributeInstance.removeModifier(this.modifierUuid);

            // Calculate and apply new value
            int level = living instanceof Player player ? LevelingUtils.getPlayerLevel(player) : 1;
            double value = this.startingValue + ((level - 1) * this.valuePerLevel);

            AttributeModifier modifier = new AttributeModifier(
                    this.modifierUuid,
                    "Leveled Attribute Bonus",
                    value,
                    this.operation
            );
            attributeInstance.addTransientModifier(modifier);

            float afterMaxHealth = living.getMaxHealth();
            if (this.updateHealth && afterMaxHealth != previousMaxHealth) {
                living.setHealth(afterMaxHealth * previousHealthPercent);
            }
        }
    }
}
