package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.selection.ClientSelectionState;
import dev.muon.raven_dnd_origins.selection.SelectionSession;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Server -&gt; client: the player's pending selection session, or absent to clear it. The client
 * caches it in {@link ClientSelectionState}; the render-tick handler opens the selection screen
 * whenever that cache holds a non-null session.
 */
public record SyncSelectionSessionMessage(@Nullable SelectionSession session) implements CustomPacketPayload {

    public static final Type<SyncSelectionSessionMessage> TYPE =
            new Type<>(RavenDndOrigins.loc("sync_selection_session"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSelectionSessionMessage> STREAM_CODEC =
            StreamCodec.of(SyncSelectionSessionMessage::write, SyncSelectionSessionMessage::read);

    private static void write(RegistryFriendlyByteBuf buf, SyncSelectionSessionMessage message) {
        buf.writeBoolean(message.session != null);
        if (message.session != null) {
            message.session.toBuf(buf);
        }
    }

    private static SyncSelectionSessionMessage read(RegistryFriendlyByteBuf buf) {
        return new SyncSelectionSessionMessage(buf.readBoolean() ? SelectionSession.fromBuf(buf) : null);
    }

    // Registered clientbound only, so this never runs on a dedicated server.
    public static void handle(SyncSelectionSessionMessage message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (message.session == null) {
                ClientSelectionState.clear();
            } else {
                ClientSelectionState.set(message.session);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
