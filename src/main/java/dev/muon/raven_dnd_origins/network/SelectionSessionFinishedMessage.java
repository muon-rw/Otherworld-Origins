package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -&gt; server: the player finished the selection screen. Triggers reconciliation, which
 * re-prompts for any still-unresolved layers or completes the session.
 */
public record SelectionSessionFinishedMessage() implements CustomPacketPayload {

    public static final Type<SelectionSessionFinishedMessage> TYPE =
            new Type<>(RavenDndOrigins.loc("selection_session_finished"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectionSessionFinishedMessage> STREAM_CODEC =
            StreamCodec.unit(new SelectionSessionFinishedMessage());

    public static void handle(SelectionSessionFinishedMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> SelectionSessions.reconcile(player));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
