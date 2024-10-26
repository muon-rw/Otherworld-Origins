package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ResetFeatsMessage {
    private static final List<ResourceLocation> FEAT_LAYERS = Arrays.asList(
            OtherworldOrigins.loc("first_feat"),
            OtherworldOrigins.loc("second_feat"),
            OtherworldOrigins.loc("third_feat"),
            OtherworldOrigins.loc("fourth_feat"),
            OtherworldOrigins.loc("fifth_feat")
    );

    public ResetFeatsMessage() {}

    public static ResetFeatsMessage decode(FriendlyByteBuf buf) {
        return new ResetFeatsMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ResetFeatsMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                IOriginContainer.get(player).ifPresent(container -> {
                    for (Holder.Reference<OriginLayer> layerRef : OriginsAPI.getActiveLayers()) {
                        layerRef.unwrapKey().ifPresent(key -> {
                            if (FEAT_LAYERS.contains(key.location())) {
                                container.setOrigin(layerRef.get(), Origin.EMPTY);
                            }
                        });
                    }
                    PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
                    OriginsCommon.CHANNEL.send(target, container.getSynchronizationPacket());
                    container.synchronize();
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void send() {
        OtherworldOrigins.CHANNEL.sendToServer(new ResetFeatsMessage());
    }
}