package dev.muon.otherworldorigins.mixin.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.network.OtherworldOriginsNetworkLimits;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Raises login-phase clientbound custom query (mod handshake) payload decode limit.
 */
@Mixin(ClientboundCustomQueryPacket.class)
public abstract class ClientboundCustomQueryPacketMixin {

    @ModifyExpressionValue(
            method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
            at = @At(value = "CONSTANT", args = "intValue=1048576")
    )
    private int otherworldorigins$raiseLoginPayloadDecode(int prior) {
        return Math.max(prior, OtherworldOriginsNetworkLimits.CUSTOM_PAYLOAD_MAX_BYTES);
    }
}
