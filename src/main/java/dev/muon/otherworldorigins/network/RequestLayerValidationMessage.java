package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RequestLayerValidationMessage {
    public RequestLayerValidationMessage() {}

    public static RequestLayerValidationMessage decode(FriendlyByteBuf buf) {
        return new RequestLayerValidationMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(RequestLayerValidationMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                OtherworldOrigins.LOGGER.info("Received layer validation request from player: {}", player.getName().getString());

                IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);
                if (originContainer != null) {
                    Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
                    List<ResourceLocation> missingOriginLayers = new ArrayList<>();

                    for (OriginLayer layer : layerRegistry) {
                        ResourceLocation layerId = layerRegistry.getKey(layer);
                        if (layerId == null) continue;

                        Holder<OriginLayer> layerHolder = layerRegistry.getHolderOrThrow(layerRegistry.getResourceKey(layer).orElseThrow());
                        ResourceKey<Origin> originKey = originContainer.getOrigin(layerHolder);

                        if (originKey == null || originKey.location().equals(new ResourceLocation("origins", "empty"))) {
                            if (!layer.origins(player).isEmpty()) {
                                missingOriginLayers.add(layerId);
                            }
                        }
                    }

                    if (!missingOriginLayers.isEmpty()) {
                        OtherworldOrigins.LOGGER.warn("Missing layers for player {}: {}", player.getName().getString(), missingOriginLayers);

                        OtherworldOrigins.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                new SendValidatedLayersMessage(missingOriginLayers)
                        );
                    } else {
                        OtherworldOrigins.LOGGER.info("Layer validation successful for player {}. All origins are properly selected.", player.getName().getString());
                    }
                } else {
                    OtherworldOrigins.LOGGER.error("Unable to get origin container for player: {}", player.getName().getString());
                }
            } else {
                OtherworldOrigins.LOGGER.error("Received layer validation request from null player");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}