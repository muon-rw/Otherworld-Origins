package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.selection.SelectionSessions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -&gt; server: the player finished the selection screen. Triggers reconciliation, which
 * re-prompts for any still-unresolved layers or completes the session.
 */
public class SelectionSessionFinishedMessage {

    public SelectionSessionFinishedMessage() {}

    public static SelectionSessionFinishedMessage decode(FriendlyByteBuf buf) {
        return new SelectionSessionFinishedMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(SelectionSessionFinishedMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                SelectionSessions.reconcile(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
