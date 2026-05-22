package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.selection.SelectionLayers;
import dev.muon.otherworldorigins.selection.SelectionSessions;
import dev.muon.otherworldorigins.selection.SessionKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -&gt; server: clear the level-gated layers and re-prompt for them. Sent on aptitude respec
 * (see {@code RespecAptitudesMessage}). Routes through {@link SelectionSessions#beginCleared} so the
 * re-pick is a persisted, relog-safe {@code LEVEL_UP} session instead of a session-less clear — the
 * latter left the player un-prompted until their next level-up and could mis-reconcile a post-respec
 * relog into full character creation.
 */
public class ResetLeveledLayersMessage {

    public ResetLeveledLayersMessage() {}

    public static ResetLeveledLayersMessage decode(FriendlyByteBuf buf) {
        return new ResetLeveledLayersMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ResetLeveledLayersMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                SelectionSessions.beginCleared(player, SelectionLayers.LEVEL_GATED, SessionKind.LEVEL_UP);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void send() {
        OtherworldOrigins.CHANNEL.sendToServer(new ResetLeveledLayersMessage());
    }
}
