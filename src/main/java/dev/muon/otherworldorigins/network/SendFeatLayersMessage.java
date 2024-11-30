package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.util.ClientLayerScreenHelper;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SendFeatLayersMessage {

    // Functionally identical to SendValidatedLayersMessage

    private final List<ResourceLocation> featLayers;

    public SendFeatLayersMessage(List<ResourceLocation> featLayers) {
        this.featLayers = featLayers;
    }

    public static SendFeatLayersMessage decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ResourceLocation> layers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            layers.add(buf.readResourceLocation());
        }
        return new SendFeatLayersMessage(layers);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(featLayers.size());
        for (ResourceLocation layer : featLayers) {
            buf.writeResourceLocation(layer);
        }
    }

    public static void handle(SendFeatLayersMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
            List<ResourceLocation> validLayerIds = new ArrayList<>();

            for (ResourceLocation layerId : message.featLayers) {
                OriginLayer layer = layerRegistry.get(layerId);
                if (layer != null) {
                    validLayerIds.add(layerId);
                }
            }

            if (!validLayerIds.isEmpty()) {
                OtherworldOrigins.LOGGER.debug("New feat(s) available. Layers:");
                for (ResourceLocation layerId : validLayerIds) {
                    OtherworldOrigins.LOGGER.debug("- " + layerId);
                }

                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientLayerScreenHelper.handleFeatLayers(validLayerIds));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}