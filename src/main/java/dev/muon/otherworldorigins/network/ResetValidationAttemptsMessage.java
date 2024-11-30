package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.util.ClientLayerScreenHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ResetValidationAttemptsMessage {
    public ResetValidationAttemptsMessage() {}

    public static ResetValidationAttemptsMessage decode(FriendlyByteBuf buf) {
        return new ResetValidationAttemptsMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ResetValidationAttemptsMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientLayerScreenHelper.resetValidationAttempts())
        );
        ctx.get().setPacketHandled(true);
    }
}