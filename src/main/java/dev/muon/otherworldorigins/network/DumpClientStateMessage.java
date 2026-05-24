package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.util.OriginStateDumper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: tells the receiving client to dump its local origin/power state to its {@code logs/}
 * directory. Sent by {@code /otherworldorigins dumpState} alongside a parallel server-side
 * dump, so both sides land snapshots simultaneously for visual-discrepancy investigations.
 *
 * <p>Mirrors {@link RequestServerStateDumpMessage}, which goes the other direction: this is
 * the server asking the client to dump, that is the client asking the server to dump.</p>
 */
public class DumpClientStateMessage {

    private final String reason;

    public DumpClientStateMessage(String reason) {
        this.reason = reason;
    }

    public static DumpClientStateMessage decode(FriendlyByteBuf buf) {
        return new DumpClientStateMessage(buf.readUtf());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(reason);
    }

    public static void handle(DumpClientStateMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    OriginStateDumper.dump(Minecraft.getInstance().player, "CLIENT", null, message.reason);
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
