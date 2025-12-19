package dev.muon.otherworldorigins.network;

import io.github.apace100.origins.screen.ChooseOriginScreen;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public class OpenOriginScreenMessage {
    private final boolean showDirtBackground;

    public OpenOriginScreenMessage(boolean showDirtBackground) {
        this.showDirtBackground = showDirtBackground;
    }

    public static OpenOriginScreenMessage decode(FriendlyByteBuf buf) {
        return new OpenOriginScreenMessage(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(showDirtBackground);
    }

    public static void handle(OpenOriginScreenMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    // Get all enabled layers that need origin selection
                    ArrayList<OriginLayer> layers = new ArrayList<>();
                    for (OriginLayer layer : OriginLayers.getLayers()) {
                        if (layer.isEnabled() && !layer.getOrigins(minecraft.player).isEmpty()) {
                            layers.add(layer);
                        }
                    }

                    if (!layers.isEmpty()) {
                        minecraft.setScreen(new ChooseOriginScreen(layers, 0, message.showDirtBackground));
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
