package dev.muon.otherworldorigins.condition.configuration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.core.Holder;

public record AnyOnLayerConfiguration(Holder<OriginLayer> layer) implements IDynamicFeatureConfiguration {
    public static final Codec<AnyOnLayerConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            OriginLayer.HOLDER_REFERENCE.fieldOf("layer").forGetter(AnyOnLayerConfiguration::layer)
    ).apply(instance, AnyOnLayerConfiguration::new));
}