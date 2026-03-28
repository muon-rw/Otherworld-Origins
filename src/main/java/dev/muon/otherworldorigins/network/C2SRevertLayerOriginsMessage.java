package dev.muon.otherworldorigins.network;

import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
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

public class C2SRevertLayerOriginsMessage {
    private final List<ResourceLocation> layersToRevert;

    public C2SRevertLayerOriginsMessage(List<ResourceLocation> layersToRevert) {
        this.layersToRevert = layersToRevert;
    }

    public static C2SRevertLayerOriginsMessage decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ResourceLocation> layers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            layers.add(buf.readResourceLocation());
        }
        return new C2SRevertLayerOriginsMessage(layers);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(layersToRevert.size());
        for (ResourceLocation layer : layersToRevert) {
            buf.writeResourceLocation(layer);
        }
    }

    public static void handle(C2SRevertLayerOriginsMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                IOriginContainer.get(player).ifPresent(container -> {
                    boolean changed = false;
                    Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.server);
                    for (ResourceLocation layerId : message.layersToRevert) {
                        ResourceKey<OriginLayer> key = ResourceKey.create(layerRegistry.key(), layerId);
                        Holder<OriginLayer> layerHolder = layerRegistry.getHolder(key).orElse(null);
                        if (layerHolder != null) {
                            container.setOrigin(layerHolder.value(), Origin.EMPTY);
                            changed = true;
                        }
                    }
                    if (changed) {
                        container.checkAutoChoosingLayers(false);
                        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
                        OriginsCommon.CHANNEL.send(target, container.getSynchronizationPacket());
                        container.synchronize();
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}