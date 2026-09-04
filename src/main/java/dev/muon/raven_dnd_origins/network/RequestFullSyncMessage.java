package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.origins.network.OriginsServerNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S "client is ready for full sync". Sent once on login, after the client's {@code LocalPlayer}
 * is installed.
 *
 * <p>Re-pushes everything to the requesting player: the power/origin/layer registries, badges,
 * origin caps, and the per-player origin + power state. This is the same set of sync paths that
 * {@code OriginsServerEvents.onPlayerLogin} already runs on login; this message lets the client
 * ask for it again once it is actually ready to receive it.</p>
 *
 * <p>The registries don't change between dimensions, so dimension change / respawn uses
 * {@link RequestContainerSyncMessage} instead — per-player state only.</p>
 */
public record RequestFullSyncMessage() implements CustomPacketPayload {

    public static final Type<RequestFullSyncMessage> TYPE = new Type<>(RavenDndOrigins.loc("request_full_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestFullSyncMessage> STREAM_CODEC =
            StreamCodec.unit(new RequestFullSyncMessage());

    public static void handle(RequestFullSyncMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> {
            OriginsServerNetwork.sendRegistries(player);
            OriginsServerNetwork.sendBadges(player);
            OriginsServerNetwork.sendOriginCaps(player);
            OriginsServerNetwork.sendPlayerOriginsTo(player, player);
            OriginsServerNetwork.sendPlayerSwapsTo(player, player);

            ApoliNetwork.sendPowers(player);
            Apoli.sendEntitySync(player);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
