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
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
                List<Holder<OriginLayer>> missingOriginLayers = new ArrayList<>();

                for (ResourceLocation layerId : message.missingLayers) {
                    OriginLayer layer = layerRegistry.get(layerId);
                    if (layer != null) {
                        missingOriginLayers.add(layerRegistry.getHolderOrThrow(layerRegistry.getResourceKey(layer).orElseThrow()));
                    }
                }

                if (!missingOriginLayers.isEmpty()) {
                    OtherworldOrigins.LOGGER.info("Server detected unpicked origins. Reopening selection screen for layers:");
                    for (Holder<OriginLayer> layerHolder : missingOriginLayers) {
                        OtherworldOrigins.LOGGER.info("- " + layerHolder.value().name().getString());
                    }
                    minecraft.execute(() -> {
                        ChooseOriginScreen newScreen = new ChooseOriginScreen(missingOriginLayers, 0, false);
                        minecraft.setScreen(newScreen);
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}