package dev.muon.otherworldorigins.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class AnyOnLayerCondition extends EntityCondition<AnyOnLayerCondition.Configuration> {
    public AnyOnLayerCondition() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean check(@NotNull Configuration configuration, @NotNull Entity entity) {
        return IOriginContainer.get(entity).resolve().map(container ->
                container.hasOrigin(configuration.layer())
        ).orElse(false);
    }

    public record Configuration(Holder<OriginLayer> layer) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                OriginLayer.HOLDER_REFERENCE.fieldOf("layer").forGetter(Configuration::layer)
        ).apply(instance, Configuration::new));
    }
}