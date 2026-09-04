package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -&gt; server: the final confirm screen's Start Over. Re-runs full character creation as a
 * fresh {@code INITIAL_CREATION} session, so the re-pick is persisted across a relog and ends in the
 * final confirm screen again. A session-less wipe leaves reconcile with nothing to complete.
 */
public record ResetOriginsMessage() implements CustomPacketPayload {
    public static final Type<ResetOriginsMessage> TYPE = new Type<>(RavenDndOrigins.loc("reset_origins"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResetOriginsMessage> STREAM_CODEC =
            StreamCodec.unit(new ResetOriginsMessage());

    public static void handle(ResetOriginsMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> SelectionSessions.beginFullCreation(player));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
