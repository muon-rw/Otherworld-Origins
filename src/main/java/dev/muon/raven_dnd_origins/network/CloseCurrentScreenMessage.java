package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CloseCurrentScreenMessage() implements CustomPacketPayload {

    public static final Type<CloseCurrentScreenMessage> TYPE = new Type<>(RavenDndOrigins.loc("close_current_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseCurrentScreenMessage> STREAM_CODEC =
            StreamCodec.unit(new CloseCurrentScreenMessage());

    // Registered clientbound only, so this never runs on a dedicated server.
    public static void handle(CloseCurrentScreenMessage message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Minecraft.getInstance().setScreen(null));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
