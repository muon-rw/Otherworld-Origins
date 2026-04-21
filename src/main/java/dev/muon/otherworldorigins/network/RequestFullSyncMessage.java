package dev.muon.otherworldorigins.network;

import io.github.apace100.calio.registry.DataObjectRegistry;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.calio.common.registry.CalioDynamicRegistryManager;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * C2S "client is ready for full sync". Sent on
 * {@link net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn} once, after the
 * client's {@code LocalPlayer} is installed.
 *
 * <p>Re-pushes <em>everything</em> to the requesting player — Calio dynamic registries (the
 * full set of defined origins / powers / layers, which can split into multiple 1 MB packets),
 * Calio {@link DataObjectRegistry} entries, and the per-player origin + power containers. This
 * is the same set of sync paths that {@link net.minecraftforge.event.OnDatapackSyncEvent}
 * triggers for a single player, deliberately re-run on a client we know is ready.</p>
 *
 * <p>The registries don't change between dimensions, so dimension change / respawn uses
 * {@link RequestContainerSyncMessage} instead — per-player containers only. That keeps the
 * per-Clone cost to tens of KB rather than hundreds.</p>
 */
public class RequestFullSyncMessage {
    public RequestFullSyncMessage() {}

    public static RequestFullSyncMessage decode(FriendlyByteBuf buf) {
        return new RequestFullSyncMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(RequestFullSyncMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);

            CalioDynamicRegistryManager.getInstance(server.registryAccess()).synchronize(target);
            DataObjectRegistry.performAutoSync(player);

            IOriginContainer.get(player).ifPresent(IOriginContainer::synchronize);
            ApoliAPI.synchronizePowerContainer(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
