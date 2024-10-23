package dev.muon.otherworldorigins.network;

import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class ResetOriginsMessage {

    public ResetOriginsMessage() {}

    public static ResetOriginsMessage decode(FriendlyByteBuf buf) {
        return new ResetOriginsMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ResetOriginsMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                IOriginContainer.get(player).ifPresent(container -> {
                    for (Holder.Reference<OriginLayer> layerRef : OriginsAPI.getActiveLayers()) {
                        container.setOrigin(layerRef.get(), Origin.EMPTY);
                    }
                    container.checkAutoChoosingLayers(false);
                    PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
                    OriginsCommon.CHANNEL.send(target, container.getSynchronizationPacket());
                    OriginsCommon.CHANNEL.send(target, new S2COpenOriginScreen(false));
                    container.synchronize();
                });
            }
        });
    }
}