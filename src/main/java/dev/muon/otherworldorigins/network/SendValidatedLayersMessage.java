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

public class SendValidatedLayersMessage {
    private final List<ResourceLocation> missingLayers;

    public SendValidatedLayersMessage(List<ResourceLocation> missingLayers) {
        this.missingLayers = missingLayers;
    }

    public static SendValidatedLayersMessage decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ResourceLocation> layers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            layers.add(buf.readResourceLocation());
        }
        return new SendValidatedLayersMessage(layers);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(missingLayers.size());
        for (ResourceLocation layer : missingLayers) {
            buf.writeResourceLocation(layer);
        }
    }

    public static void handle(SendValidatedLayersMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
            List<ResourceLocation> validMissingLayers = new ArrayList<>();

            for (ResourceLocation layerId : message.missingLayers) {
                OriginLayer layer = layerRegistry.get(layerId);
                if (layer != null) {
                    validMissingLayers.add(layerId);
                }
            }

            if (!validMissingLayers.isEmpty()) {
                OtherworldOrigins.LOGGER.info("Server detected unpicked origins. Layers:");
                for (ResourceLocation layerId : validMissingLayers) {
                    OtherworldOrigins.LOGGER.info("- " + layerId);
                }

                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientLayerScreenHelper.handleValidatedLayers(validMissingLayers));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}