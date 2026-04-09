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

public class SendLeveledLayersMessage {

    // Functionally identical to SendValidatedLayersMessage

    private final List<ResourceLocation> leveledLayers;

    public SendLeveledLayersMessage(List<ResourceLocation> leveledLayers) {
        this.leveledLayers = leveledLayers;
    }

    public static SendLeveledLayersMessage decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ResourceLocation> layers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            layers.add(buf.readResourceLocation());
        }
        return new SendLeveledLayersMessage(layers);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(leveledLayers.size());
        for (ResourceLocation layer : leveledLayers) {
            buf.writeResourceLocation(layer);
        }
    }

    public static void handle(SendLeveledLayersMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
            List<ResourceLocation> validLayerIds = new ArrayList<>();

            for (ResourceLocation layerId : message.leveledLayers) {
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

                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientLayerScreenHelper.handleLeveledLayers(validLayerIds));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}