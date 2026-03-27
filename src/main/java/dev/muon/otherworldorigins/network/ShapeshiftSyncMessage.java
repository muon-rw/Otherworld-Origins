package dev.muon.otherworldorigins.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Sent server -> client to inform all tracking clients of a player's shapeshift state.
 * A null entityType means the shapeshift has ended.
 */
public record ShapeshiftSyncMessage(
        int playerId,
        @Nullable ResourceLocation entityType,
        boolean hideHands,
        boolean allowTools,
        float collisionWidth,
        float collisionHeight
) {

    public static ShapeshiftSyncMessage decode(FriendlyByteBuf buf) {
        int playerId = buf.readVarInt();
        boolean hasType = buf.readBoolean();
        if (!hasType) {
            boolean hideHands = buf.readBoolean();
            boolean allowTools = buf.readBoolean();
            return new ShapeshiftSyncMessage(playerId, null, hideHands, allowTools, 0.0F, 0.0F);
        }
        ResourceLocation type = buf.readResourceLocation();
        boolean hideHands = buf.readBoolean();
        boolean allowTools = buf.readBoolean();
        float cW = buf.readFloat();
        float cH = buf.readFloat();
        return new ShapeshiftSyncMessage(playerId, type, hideHands, allowTools, cW, cH);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(playerId);
        if (entityType == null) {
            buf.writeBoolean(false);
            buf.writeBoolean(hideHands);
            buf.writeBoolean(allowTools);
            return;
        }
        buf.writeBoolean(true);
        buf.writeResourceLocation(entityType);
        buf.writeBoolean(hideHands);
        buf.writeBoolean(allowTools);
        buf.writeFloat(collisionWidth);
        buf.writeFloat(collisionHeight);
    }

    public static void handle(ShapeshiftSyncMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Consumer<ShapeshiftSyncMessage> client = ShapeshiftSyncClientDispatch.getHandler();
            if (client != null) {
                client.accept(message);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
