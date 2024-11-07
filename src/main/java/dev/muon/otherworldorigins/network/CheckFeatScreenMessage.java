package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.util.Utils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CheckFeatScreenMessage {

    public CheckFeatScreenMessage() {}

    public static void encode(CheckFeatScreenMessage message, FriendlyByteBuf buffer) {}

    public static CheckFeatScreenMessage decode(FriendlyByteBuf buffer) {
        return new CheckFeatScreenMessage();
    }

    public static void handle(CheckFeatScreenMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Utils.checkForFeats(player);
            }
        });
        context.setPacketHandled(true);
    }
}