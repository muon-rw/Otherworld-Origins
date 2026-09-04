package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.origins.network.OriginsServerNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S "client is ready for per-player container sync". Sent on dimension change / respawn, once
 * the client has installed the replacement {@code LocalPlayer}.
 *
 * <p>This message only re-sends the per-player origin picks and power state (tens of KB). For a
 * full re-sync of the registries (the "set of all defined powers / origins / layers", hundreds of
 * KB) see {@link RequestFullSyncMessage}, sent once on login — those registries don't change
 * between dimensions so we don't re-push them on every portal transition.</p>
 */
public record RequestContainerSyncMessage() implements CustomPacketPayload {

    public static final Type<RequestContainerSyncMessage> TYPE =
            new Type<>(RavenDndOrigins.loc("request_container_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestContainerSyncMessage> STREAM_CODEC =
            StreamCodec.unit(new RequestContainerSyncMessage());

    public static void handle(RequestContainerSyncMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            OriginsServerNetwork.broadcastPlayerOrigins(server, player);
            OriginsServerNetwork.sendPlayerSwapsTo(player, player);
            Apoli.sendEntitySync(player);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
