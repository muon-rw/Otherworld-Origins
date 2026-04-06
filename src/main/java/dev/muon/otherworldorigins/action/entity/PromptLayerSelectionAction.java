package dev.muon.otherworldorigins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import dev.muon.otherworldorigins.network.SendFeatLayersMessage;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

public class PromptLayerSelectionAction extends EntityAction<PromptLayerSelectionAction.Configuration> {

    public PromptLayerSelectionAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.level().getServer());

        List<ResourceLocation> validLayers = configuration.layers().stream()
                .filter(layerId -> {
                    if (layerRegistry.get(layerId) == null) {
                        OtherworldOrigins.LOGGER.error("prompt_layer_selection: layer '{}' does not exist", layerId);
                        return false;
                    }
                    return true;
                })
                .toList();

        if (validLayers.isEmpty()) return;

        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
        OtherworldOrigins.CHANNEL.send(target, new CloseCurrentScreenMessage());
        OtherworldOrigins.CHANNEL.send(target, new SendFeatLayersMessage(validLayers));
    }

    public record Configuration(List<ResourceLocation> layers) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("layers").forGetter(Configuration::layers)
        ).apply(instance, Configuration::new));
    }
}
