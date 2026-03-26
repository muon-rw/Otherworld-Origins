package dev.muon.otherworldorigins.mixin.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.network.OtherworldOriginsNetworkLimits;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Raises the play-phase custom payload size checks (encode and decode) to fit large Calio registry sync.
 * Uses {@link Math#max} so other mods' chained {@link ModifyExpressionValue} injectors can set a higher ceiling.
 */
@Mixin(ClientboundCustomPayloadPacket.class)
public abstract class ClientboundCustomPayloadPacketMixin {

    @ModifyExpressionValue(
            method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)V",
            at = @At(value = "CONSTANT", args = "intValue=1048576")
    )
    private int otherworldorigins$raiseOutgoingCheck(int prior) {
        return Math.max(prior, OtherworldOriginsNetworkLimits.CUSTOM_PAYLOAD_MAX_BYTES);
    }

    @ModifyExpressionValue(
            method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
            at = @At(value = "CONSTANT", args = "intValue=1048576")
    )
    private int otherworldorigins$raiseIncomingCheck(int prior) {
        return Math.max(prior, OtherworldOriginsNetworkLimits.CUSTOM_PAYLOAD_MAX_BYTES);
    }
}
