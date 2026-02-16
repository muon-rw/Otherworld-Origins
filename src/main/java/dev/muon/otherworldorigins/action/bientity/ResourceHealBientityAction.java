package dev.muon.otherworldorigins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.configuration.PowerReference;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.OptionalInt;

public class ResourceHealBientityAction extends BiEntityAction<ResourceHealBientityAction.Configuration> {

    public ResourceHealBientityAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(target instanceof LivingEntity livingTarget) || actor.level().isClientSide()) {
            return;
        }

        var powerHolder = IPowerContainer.get(actor).resolve()
                .flatMap(c -> Optional.ofNullable(c.getPower(configuration.resource().power())))
                .filter(Holder::isBound)
                .orElse(null);
        if (powerHolder == null) {
            return;
        }

        ConfiguredPower<?, ?> power = powerHolder.value();
        OptionalInt valueOpt = power.getValue(actor);
        if (valueOpt.isEmpty()) {
            return;
        }
        int available = valueOpt.getAsInt();
        if (available <= 0) {
            return;
        }

        float missingHealth = livingTarget.getMaxHealth() - livingTarget.getHealth();
        int healAmount = (int) Math.min(missingHealth, available);
        if (healAmount <= 0) {
            return;
        }

        livingTarget.heal(healAmount);
        power.assign(actor, available - healAmount);
        if (actor instanceof Player player) {
            ApoliAPI.synchronizePowerContainer(player);
        }
    }

    public record Configuration(PowerReference resource) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PowerReference.mapCodec("resource").forGetter(Configuration::resource)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
