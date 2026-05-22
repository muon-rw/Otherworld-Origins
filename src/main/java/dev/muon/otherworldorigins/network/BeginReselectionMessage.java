package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.selection.SelectionSessions;
import dev.muon.otherworldorigins.selection.SessionKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Client -&gt; server: re-pick a set of layers — from the inventory "Choose Origin" button or the
 * scoped confirm screen's "Re-pick". Clears the layers and opens a RESELECTION session.
 */
public class BeginReselectionMessage {

    private final List<ResourceLocation> layers;

    public BeginReselectionMessage(List<ResourceLocation> layers) {
        this.layers = layers;
    }

    public static BeginReselectionMessage decode(FriendlyByteBuf buf) {
        return new BeginReselectionMessage(buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeCollection(layers, FriendlyByteBuf::writeResourceLocation);
    }

    public static void handle(BeginReselectionMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                SelectionSessions.beginCleared(player, message.layers, SessionKind.RESELECTION);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
