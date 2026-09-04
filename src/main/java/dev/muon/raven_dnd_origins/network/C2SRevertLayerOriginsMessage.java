package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.overgrown.origins.network.OriginsServerNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Client -&gt; server: the selection screen stepped back to an earlier layer, so that layer and
 * every later one are cleared for re-picking. Batched through {@link SelectionSessions#clearLayers}
 * so Origins reconciles once after the whole range is empty.
 */
public record C2SRevertLayerOriginsMessage(List<ResourceLocation> layersToRevert) implements CustomPacketPayload {
    public static final Type<C2SRevertLayerOriginsMessage> TYPE =
            new Type<>(RavenDndOrigins.loc("c2s_revert_layer_origins"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRevertLayerOriginsMessage> STREAM_CODEC =
            StreamCodec.of(C2SRevertLayerOriginsMessage::write, C2SRevertLayerOriginsMessage::read);

    private static void write(RegistryFriendlyByteBuf buf, C2SRevertLayerOriginsMessage message) {
        buf.writeCollection(message.layersToRevert, FriendlyByteBuf::writeResourceLocation);
    }

    private static C2SRevertLayerOriginsMessage read(RegistryFriendlyByteBuf buf) {
        return new C2SRevertLayerOriginsMessage(buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    public static void handle(C2SRevertLayerOriginsMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> {
            if (SelectionSessions.clearLayers(player, message.layersToRevert).isEmpty()) {
                return;
            }
            MinecraftServer server = player.getServer();
            if (server != null) {
                OriginsServerNetwork.broadcastPlayerOrigins(server, player);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
