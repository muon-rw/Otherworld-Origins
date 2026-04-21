package dev.muon.otherworldorigins.network;

import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S "client is ready for per-player container sync". Sent on
 * {@link net.minecraftforge.client.event.ClientPlayerNetworkEvent.Clone} — the event that
 * fires for both dimension change and respawn, once the client has installed the replacement
 * {@code LocalPlayer}.
 *
 * <p>Server-initiated per-player syncs route through
 * {@code PacketDistributor.TRACKING_ENTITY_AND_SELF}, which keys on the player's entity id.
 * If the packet arrives before the new {@code LocalPlayer} is added to the new
 * {@code ClientLevel}, the client-side {@code level.getEntity(id)} lookup returns {@code null}
 * and the packet is silently dropped, leaving the client with a stale or empty power / origin
 * container until the next natural sync trigger. Having the client request the re-sync only
 * once it's ready sidesteps the race.</p>
 *
 * <p>This message only re-sends the per-player containers (tens of KB). For a full re-sync of
 * the Calio dynamic registries (the "set of all defined powers / origins / layers", hundreds of
 * KB) see {@link RequestFullSyncMessage}, sent once on login — those registries don't change
 * between dimensions so we don't re-push them on every portal transition.</p>
 */
public class RequestContainerSyncMessage {
    public RequestContainerSyncMessage() {}

    public static RequestContainerSyncMessage decode(FriendlyByteBuf buf) {
        return new RequestContainerSyncMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(RequestContainerSyncMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            IOriginContainer.get(player).ifPresent(IOriginContainer::synchronize);
            ApoliAPI.synchronizePowerContainer(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
