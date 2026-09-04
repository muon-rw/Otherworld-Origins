package dev.muon.raven_dnd_origins.mixin.compat.shouldersurfing;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.raven_dnd_origins.client.compat.ShoulderSurfingIntegration;
import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import dev.overgrown.apoli.client.ApoliKeyHandler;
import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * In shoulder surfing the body faces wherever the camera left it, so a directional active power
 * would fire away from the crosshair. Rotate the player onto the crosshair target before the
 * activation packet leaves the client, so the server sees the corrected rotation first.
 */
@Mixin(value = ApoliKeyHandler.class, remap = false)
public class ApoliKeyHandlerMixin {

    @WrapOperation(
            method = "onClientTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/network/PacketDistributor;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"
            )
    )
    private static void raven_dnd_origins$faceCrosshairBeforePowerActivation(
            CustomPacketPayload payload, CustomPacketPayload[] payloads, Operation<Void> original) {
        if (payload instanceof PowerActivationC2S activation
                && !RavenDndOriginsConfig.isPowerRotationBlacklisted(activation.power().toString())) {
            ShoulderSurfingIntegration.lookAtCrosshairTargetIfShoulderSurfing();
        }
        original.call(payload, payloads);
    }
}
