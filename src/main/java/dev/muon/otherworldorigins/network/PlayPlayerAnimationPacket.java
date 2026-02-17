package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.client.PlayerAnimationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record PlayPlayerAnimationPacket(UUID playerId, ResourceLocation animation) {

    public static PlayPlayerAnimationPacket decode(FriendlyByteBuf buf) {
        return new PlayPlayerAnimationPacket(buf.readUUID(), buf.readResourceLocation());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeResourceLocation(animation);
    }

    public static void handle(PlayPlayerAnimationPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        PlayerAnimationHandler.playAnimation(message.playerId(), message.animation()))
        );
        ctx.get().setPacketHandled(true);
    }
}
