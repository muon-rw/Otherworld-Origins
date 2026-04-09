package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.util.LeveledLayerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CheckLeveledLayersMessage {

    public CheckLeveledLayersMessage() {}

    public static void encode(CheckLeveledLayersMessage message, FriendlyByteBuf buffer) {}

    public static CheckLeveledLayersMessage decode(FriendlyByteBuf buffer) {
        return new CheckLeveledLayersMessage();
    }

    public static void handle(CheckLeveledLayersMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                LeveledLayerHandler.checkForEmptyValidLayers(player);
            }
        });
        context.setPacketHandled(true);
    }
}