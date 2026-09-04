package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.selection.SelectionLayers;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -&gt; server: clear the level-gated layers and re-prompt for them. Sent on aptitude respec
 * (see {@code RespecAptitudesMessage}). Routes through {@link SelectionSessions#beginCleared} so the
 * re-pick is a persisted, relog-safe {@code LEVEL_UP} session instead of a session-less clear — the
 * latter left the player un-prompted until their next level-up and could mis-reconcile a post-respec
 * relog into full character creation.
 */
public record ResetLeveledLayersMessage() implements CustomPacketPayload {

    public static final Type<ResetLeveledLayersMessage> TYPE =
            new Type<>(RavenDndOrigins.loc("reset_leveled_layers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResetLeveledLayersMessage> STREAM_CODEC =
            StreamCodec.unit(new ResetLeveledLayersMessage());

    public static void handle(ResetLeveledLayersMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() ->
                SelectionSessions.beginCleared(player, SelectionLayers.LEVEL_GATED, SessionKind.LEVEL_UP));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
