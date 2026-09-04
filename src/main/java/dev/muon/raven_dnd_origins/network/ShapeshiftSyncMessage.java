package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Sent server -&gt; client to inform all tracking clients of a player's shapeshift state.
 * A null entityType means the shapeshift has ended.
 */
public record ShapeshiftSyncMessage(
        int playerId,
        @Nullable ResourceLocation entityType,
        boolean hideHands,
        boolean allowTools,
        float collisionWidth,
        float collisionHeight,
        float flightSpeed
) implements CustomPacketPayload {

    public static final Type<ShapeshiftSyncMessage> TYPE = new Type<>(RavenDndOrigins.loc("shapeshift_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShapeshiftSyncMessage> STREAM_CODEC =
            StreamCodec.of(ShapeshiftSyncMessage::write, ShapeshiftSyncMessage::read);

    private static void write(RegistryFriendlyByteBuf buf, ShapeshiftSyncMessage message) {
        buf.writeVarInt(message.playerId);
        if (message.entityType == null) {
            buf.writeBoolean(false);
            buf.writeBoolean(message.hideHands);
            buf.writeBoolean(message.allowTools);
            return;
        }
        buf.writeBoolean(true);
        buf.writeResourceLocation(message.entityType);
        buf.writeBoolean(message.hideHands);
        buf.writeBoolean(message.allowTools);
        buf.writeFloat(message.collisionWidth);
        buf.writeFloat(message.collisionHeight);
        buf.writeFloat(message.flightSpeed);
    }

    private static ShapeshiftSyncMessage read(RegistryFriendlyByteBuf buf) {
        int playerId = buf.readVarInt();
        boolean hasType = buf.readBoolean();
        if (!hasType) {
            boolean hideHands = buf.readBoolean();
            boolean allowTools = buf.readBoolean();
            return new ShapeshiftSyncMessage(playerId, null, hideHands, allowTools, 0.0F, 0.0F, 1.0F);
        }
        ResourceLocation type = buf.readResourceLocation();
        boolean hideHands = buf.readBoolean();
        boolean allowTools = buf.readBoolean();
        float collisionWidth = buf.readFloat();
        float collisionHeight = buf.readFloat();
        float flightSpeed = buf.readFloat();
        return new ShapeshiftSyncMessage(playerId, type, hideHands, allowTools, collisionWidth, collisionHeight, flightSpeed);
    }

    // Registered clientbound only, so this never runs on a dedicated server.
    public static void handle(ShapeshiftSyncMessage message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Consumer<ShapeshiftSyncMessage> client = ShapeshiftSyncClientDispatch.getHandler();
            if (client != null) {
                client.accept(message);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
