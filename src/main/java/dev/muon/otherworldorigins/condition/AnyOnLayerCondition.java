package dev.muon.otherworldorigins.condition;

import dev.muon.otherworldorigins.condition.configuration.AnyOnLayerConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class AnyOnLayerCondition extends EntityCondition<AnyOnLayerConfiguration> {
    public AnyOnLayerCondition() {
        super(AnyOnLayerConfiguration.CODEC);
    }

    @Override
    public boolean check(@NotNull AnyOnLayerConfiguration configuration, @NotNull Entity entity) {
        return IOriginContainer.get(entity).resolve().map(container ->
                container.hasOrigin(configuration.layer())
        ).orElse(false);
    }
}