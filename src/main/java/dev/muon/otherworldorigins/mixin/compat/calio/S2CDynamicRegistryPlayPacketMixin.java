package dev.muon.otherworldorigins.mixin.compat.calio;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.network.OtherworldOriginsNetworkLimits;
import io.github.edwinmindcraft.calio.common.network.packet.S2CDynamicRegistryPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = S2CDynamicRegistryPacket.Play.class, remap = false)
public abstract class S2CDynamicRegistryPlayPacketMixin {

    @ModifyExpressionValue(
            method = "create",
            at = @At(value = "CONSTANT", args = "intValue=1048576")
    )
    private static int otherworldorigins$raisePlaySplitLimit(int prior) {
        return Math.max(prior, OtherworldOriginsNetworkLimits.CALIO_DYNAMIC_REGISTRY_CHUNK_SOFT_LIMIT);
    }
}
