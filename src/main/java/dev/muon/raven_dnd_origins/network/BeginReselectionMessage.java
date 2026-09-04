package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Client -&gt; server: re-pick a set of layers — from the inventory "Choose Origin" button or the
 * scoped confirm screen's "Re-pick". Clears the layers and opens a RESELECTION session.
 */
public record BeginReselectionMessage(List<ResourceLocation> layers) implements CustomPacketPayload {

    public static final Type<BeginReselectionMessage> TYPE = new Type<>(RavenDndOrigins.loc("begin_reselection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BeginReselectionMessage> STREAM_CODEC = StreamCodec.of(
            BeginReselectionMessage::write, BeginReselectionMessage::read);

    private static void write(RegistryFriendlyByteBuf buf, BeginReselectionMessage message) {
        buf.writeCollection(message.layers, FriendlyByteBuf::writeResourceLocation);
    }

    private static BeginReselectionMessage read(RegistryFriendlyByteBuf buf) {
        return new BeginReselectionMessage(buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    public static void handle(BeginReselectionMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> SelectionSessions.beginCleared(player, message.layers(), SessionKind.RESELECTION));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
