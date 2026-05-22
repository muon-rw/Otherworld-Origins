package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.client.screen.FinalConfirmScreen;
import dev.muon.otherworldorigins.client.screen.ScopedConfirmScreen;
import dev.muon.otherworldorigins.selection.SessionKind;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Server -&gt; client: a selection session completed; open its confirmation screen.
 * {@code INITIAL_CREATION} gets the full screen; {@code RESELECTION} gets the scoped screen,
 * limited to the layers that were re-picked.
 */
public class OpenFinalConfirmScreenMessage {

    private final SessionKind kind;
    private final List<ResourceLocation> layers;

    public OpenFinalConfirmScreenMessage(SessionKind kind, List<ResourceLocation> layers) {
        this.kind = kind;
        this.layers = layers;
    }

    public static OpenFinalConfirmScreenMessage decode(FriendlyByteBuf buf) {
        SessionKind kind = buf.readEnum(SessionKind.class);
        return new OpenFinalConfirmScreenMessage(kind, buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(kind);
        buf.writeCollection(layers, FriendlyByteBuf::writeResourceLocation);
    }

    public static void handle(OpenFinalConfirmScreenMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    if (message.kind == SessionKind.RESELECTION) {
                        Minecraft.getInstance().setScreen(new ScopedConfirmScreen(message.layers));
                    } else {
                        Minecraft.getInstance().setScreen(new FinalConfirmScreen());
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
