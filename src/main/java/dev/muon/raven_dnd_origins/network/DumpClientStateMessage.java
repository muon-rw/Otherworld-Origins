package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.client.ClientOriginStateDump;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: tells the receiving client to dump its local origin/power state to its {@code logs/}
 * directory. Sent by {@code /raven_dnd_origins dumpState} alongside a parallel server-side
 * dump, so both sides land snapshots simultaneously for visual-discrepancy investigations.
 *
 * <p>Mirrors {@link RequestServerStateDumpMessage}, which goes the other direction: this is
 * the server asking the client to dump, that is the client asking the server to dump.</p>
 */
public record DumpClientStateMessage(String reason) implements CustomPacketPayload {

    public static final Type<DumpClientStateMessage> TYPE = new Type<>(RavenDndOrigins.loc("dump_client_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DumpClientStateMessage> STREAM_CODEC = StreamCodec.of(
            DumpClientStateMessage::write, DumpClientStateMessage::read);

    private static void write(RegistryFriendlyByteBuf buf, DumpClientStateMessage message) {
        buf.writeUtf(message.reason);
    }

    private static DumpClientStateMessage read(RegistryFriendlyByteBuf buf) {
        return new DumpClientStateMessage(buf.readUtf());
    }

    // Registered clientbound only, so this never runs on a dedicated server.
    public static void handle(DumpClientStateMessage message, IPayloadContext ctx) {
        ctx.enqueueWork(() ->
                ClientOriginStateDump.dump(message.reason()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
