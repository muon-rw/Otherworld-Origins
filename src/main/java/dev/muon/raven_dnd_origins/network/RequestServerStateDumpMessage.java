package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.util.OriginStateDumper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: client is about to disconnect via the validation-loop failsafe and is asking the
 * server to dump its parallel view of the registries and this player's containers so we
 * can compare what each side saw at the moment of failure.
 *
 * <p>The client dumps locally and sends this packet immediately before calling
 * {@code disconnect(...)}; the server-side dump lands in the dedicated server's
 * {@code logs/} directory under {@code ow-origins-statedump-server-*.txt}.</p>
 */
public record RequestServerStateDumpMessage() implements CustomPacketPayload {

    public static final Type<RequestServerStateDumpMessage> TYPE =
            new Type<>(RavenDndOrigins.loc("request_server_state_dump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestServerStateDumpMessage> STREAM_CODEC =
            StreamCodec.unit(new RequestServerStateDumpMessage());

    public static void handle(RequestServerStateDumpMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> OriginStateDumper.dump(player, "SERVER", player.getServer(),
                "client requested dump prior to validation-failure disconnect"));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
