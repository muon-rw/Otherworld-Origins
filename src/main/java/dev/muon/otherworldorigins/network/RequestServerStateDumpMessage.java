package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.util.OriginStateDumper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: client is about to disconnect via the validation-loop failsafe and is asking the
 * server to dump its parallel view of the registries and this player's containers so we
 * can compare what each side saw at the moment of failure.
 *
 * <p>The client dumps locally and sends this packet immediately before calling
 * {@code disconnect(...)}; the server-side dump lands in the dedicated server's
 * {@code logs/} directory under {@code ow-origins-statedump-server-*.txt}.</p>
 */
public class RequestServerStateDumpMessage {
    public RequestServerStateDumpMessage() {}

    public static RequestServerStateDumpMessage decode(FriendlyByteBuf buf) {
        return new RequestServerStateDumpMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(RequestServerStateDumpMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            OriginStateDumper.dump(player, "SERVER", player.getServer(),
                    "client requested dump prior to validation-failure disconnect");
        });
        ctx.get().setPacketHandled(true);
    }
}
