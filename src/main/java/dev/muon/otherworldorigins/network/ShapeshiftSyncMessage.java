package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Sent server -> client to inform all tracking clients of a player's shapeshift state.
 * A null entityType means the shapeshift has ended.
 */
public record ShapeshiftSyncMessage(
        int playerId,
        @Nullable ResourceLocation entityType,
        boolean hideHands,
        boolean allowTools
) {

    public static ShapeshiftSyncMessage decode(FriendlyByteBuf buf) {
        int playerId = buf.readVarInt();
        boolean hasType = buf.readBoolean();
        ResourceLocation type = hasType ? buf.readResourceLocation() : null;
        boolean hideHands = buf.readBoolean();
        boolean allowTools = buf.readBoolean();
        return new ShapeshiftSyncMessage(playerId, type, hideHands, allowTools);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(playerId);
        buf.writeBoolean(entityType != null);
        if (entityType != null) {
            buf.writeResourceLocation(entityType);
        }
        buf.writeBoolean(hideHands);
        buf.writeBoolean(allowTools);
    }

    public static void handle(ShapeshiftSyncMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ShapeshiftClientState.handleSync(
                                message.playerId(), message.entityType(),
                                message.hideHands(), message.allowTools()))
        );
        ctx.get().setPacketHandled(true);
    }
}
