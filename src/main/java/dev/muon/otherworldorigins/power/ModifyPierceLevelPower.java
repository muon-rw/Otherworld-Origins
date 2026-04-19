package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.ListConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredModifier;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.power.IValueModifyingPowerConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.power.ValueModifyingPowerFactory;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;

public class ModifyPierceLevelPower extends ValueModifyingPowerFactory<ModifyPierceLevelPower.Configuration> {

    public ModifyPierceLevelPower() {
        super(Configuration.CODEC);
    }

    public static float modify(Entity entity, float baseValue) {
        return IPowerContainer.modify(entity, ModPowers.MODIFY_PIERCE_LEVEL.get(), baseValue);
    }

    public static boolean hasPower(Entity entity) {
        return IPowerContainer.get(entity)
                .map(container -> !container.getPowers(ModPowers.MODIFY_PIERCE_LEVEL.get()).isEmpty())
                .orElse(false);
    }

    public static void executeActions(Entity entity) {
        IPowerContainer.get(entity).ifPresent(container -> {
            for (Holder<ConfiguredPower<Configuration, ModifyPierceLevelPower>> holder :
                    container.getPowers(ModPowers.MODIFY_PIERCE_LEVEL.get())) {
                if (holder.isBound() && holder.value().isActive(entity)) {
                    ConfiguredEntityAction.execute(holder.value().getConfiguration().selfAction(), entity);
                }
            }
        });
    }

    public record Configuration(
            ListConfiguration<ConfiguredModifier<?>> modifiers,
            Holder<ConfiguredEntityAction<?, ?>> selfAction
    ) implements IValueModifyingPowerConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ListConfiguration.MODIFIER_CODEC.forGetter(Configuration::modifiers),
                ConfiguredEntityAction.optional("self_action").forGetter(Configuration::selfAction)
        ).apply(instance, Configuration::new));
    }
}
