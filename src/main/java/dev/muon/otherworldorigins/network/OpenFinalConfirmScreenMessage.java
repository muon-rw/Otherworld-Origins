package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.client.screen.FinalConfirmScreen;
import dev.muon.otherworldorigins.util.ClientLayerScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenFinalConfirmScreenMessage {
    public OpenFinalConfirmScreenMessage() {}

    public static OpenFinalConfirmScreenMessage decode(FriendlyByteBuf buf) {
        return new OpenFinalConfirmScreenMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(OpenFinalConfirmScreenMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    // Check if we should show the final confirm screen before clearing
                    boolean shouldShowConfirm = !ClientLayerScreenHelper.wasOnlyFeatLayersSelected();
                    
                    // Always clear tracked layers to prevent state leakage
                    ClientLayerScreenHelper.clearSelectedLayers();
                    
                    if (shouldShowConfirm) {
                        ClientLayerScreenHelper.setFinalConfirmScreen();
                        /*
                        Minecraft minecraft = Minecraft.getInstance();
                        minecraft.execute(() -> minecraft.setScreen(new FinalConfirmScreen()));

                         */
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}