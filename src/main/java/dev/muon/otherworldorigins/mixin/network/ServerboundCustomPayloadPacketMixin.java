package dev.muon.otherworldorigins.mixin.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.network.OtherworldOriginsNetworkLimits;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Raises play-phase serverbound custom payload decode limit (vanilla 32 KiB).
 */
@Mixin(ServerboundCustomPayloadPacket.class)
public abstract class ServerboundCustomPayloadPacketMixin {

    @ModifyExpressionValue(
            method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
            at = @At(value = "CONSTANT", args = "intValue=32767")
    )
    private int otherworldorigins$raiseServerboundDecode(int prior) {
        return Math.max(prior, OtherworldOriginsNetworkLimits.CUSTOM_PAYLOAD_MAX_BYTES);
    }
}
