package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.client.PlayerAnimationHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record PlayPlayerAnimationPacket(UUID playerId, ResourceLocation animation) implements CustomPacketPayload {

    public static final Type<PlayPlayerAnimationPacket> TYPE =
            new Type<>(RavenDndOrigins.loc("play_player_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayPlayerAnimationPacket> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, PlayPlayerAnimationPacket::playerId,
                    ResourceLocation.STREAM_CODEC, PlayPlayerAnimationPacket::animation,
                    PlayPlayerAnimationPacket::new);

    // Registered clientbound only, so this never runs on a dedicated server.
    public static void handle(PlayPlayerAnimationPacket message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> PlayerAnimationHandler.playAnimation(message.playerId(), message.animation()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
