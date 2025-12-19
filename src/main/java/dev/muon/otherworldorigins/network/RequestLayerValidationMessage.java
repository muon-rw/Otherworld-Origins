package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import net.minecraft.network.FriendlyByteBuf;
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

                OriginComponent originComponent = ModComponents.ORIGIN.maybeGet(player).orElse(null);
                if (originComponent != null) {
                    List<ResourceLocation> missingOriginLayers = new ArrayList<>();

                    for (OriginLayer layer : OriginLayers.getLayers()) {
                        if (!layer.isEnabled()) continue;

                        ResourceLocation layerId = layer.getIdentifier();
                        Origin currentOrigin = originComponent.getOrigin(layer);

                        if (currentOrigin == null || currentOrigin == Origin.EMPTY) {
                            List<ResourceLocation> availableOrigins = layer.getOrigins(player);
                            if (!availableOrigins.isEmpty()) {
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
                        
                        // Always reset validation attempts
                        OtherworldOrigins.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                new ResetValidationAttemptsMessage()
                        );
                        
                        // Always send the open confirm screen message - client will decide whether to show it
                        OtherworldOrigins.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                new OpenFinalConfirmScreenMessage()
                        );
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
