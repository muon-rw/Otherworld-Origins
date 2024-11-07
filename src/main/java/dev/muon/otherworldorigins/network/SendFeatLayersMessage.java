package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
                List<Holder<OriginLayer>> featOriginLayers = new ArrayList<>();

                for (ResourceLocation layerId : message.featLayers) {
                    OriginLayer layer = layerRegistry.get(layerId);
                    if (layer != null) {
                        featOriginLayers.add(layerRegistry.getHolderOrThrow(layerRegistry.getResourceKey(layer).orElseThrow()));
                    }
                }

                if (!featOriginLayers.isEmpty()) {
                    OtherworldOrigins.LOGGER.debug("New feat(s) available. Opening selection screen for layers:");
                    for (Holder<OriginLayer> layerHolder : featOriginLayers) {
                        OtherworldOrigins.LOGGER.debug("- " + layerHolder.value().name().getString());
                    }
                    minecraft.execute(() -> {
                        ChooseOriginScreen newScreen = new ChooseOriginScreen(featOriginLayers, 0, false);
                        minecraft.setScreen(newScreen);
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}