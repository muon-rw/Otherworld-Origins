package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.selection.ClientSelectionState;
import dev.muon.otherworldorigins.selection.SelectionSession;
import io.github.edwinmindcraft.origins.client.OriginsClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Server -&gt; client: the player's pending selection session, or absent to clear it. When present,
 * raises {@link OriginsClient#AWAITING_DISPLAY} so the render-tick handler opens the screen from
 * the synced session instead of guessing from container state.
 */
public class SyncSelectionSessionMessage {

    @Nullable
    private final SelectionSession session;

    public SyncSelectionSessionMessage(@Nullable SelectionSession session) {
        this.session = session;
    }

    public static SyncSelectionSessionMessage decode(FriendlyByteBuf buf) {
        return new SyncSelectionSessionMessage(buf.readBoolean() ? SelectionSession.fromBuf(buf) : null);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(session != null);
        if (session != null) {
            session.toBuf(buf);
        }
    }

    public static void handle(SyncSelectionSessionMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    if (message.session == null) {
                        ClientSelectionState.clear();
                        OriginsClient.AWAITING_DISPLAY.set(false);
                    } else {
                        ClientSelectionState.set(message.session);
                        OriginsClient.AWAITING_DISPLAY.set(true);
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
