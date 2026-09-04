package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.client.screen.ConfirmScreens;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server -&gt; client: a selection session completed; open its confirmation screen.
 * {@code INITIAL_CREATION} gets the full screen; {@code RESELECTION} gets the scoped screen,
 * limited to the layers that were re-picked.
 */
public record OpenFinalConfirmScreenMessage(SessionKind kind, List<ResourceLocation> layers)
        implements CustomPacketPayload {

    public static final Type<OpenFinalConfirmScreenMessage> TYPE =
            new Type<>(RavenDndOrigins.loc("open_final_confirm_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenFinalConfirmScreenMessage> STREAM_CODEC =
            StreamCodec.of(OpenFinalConfirmScreenMessage::write, OpenFinalConfirmScreenMessage::read);

    private static void write(RegistryFriendlyByteBuf buf, OpenFinalConfirmScreenMessage message) {
        buf.writeEnum(message.kind);
        buf.writeCollection(message.layers, FriendlyByteBuf::writeResourceLocation);
    }

    private static OpenFinalConfirmScreenMessage read(RegistryFriendlyByteBuf buf) {
        SessionKind kind = buf.readEnum(SessionKind.class);
        return new OpenFinalConfirmScreenMessage(kind, buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    // Registered clientbound only, so this never runs on a dedicated server.
    public static void handle(OpenFinalConfirmScreenMessage message, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ConfirmScreens.open(message.kind, message.layers);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
