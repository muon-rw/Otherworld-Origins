package dev.muon.otherworldorigins.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftClientState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$hideShapeshiftedHands(
            float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource buffer,
            LocalPlayer player, int combinedLight, CallbackInfo ci
    ) {
        if (player != null && ShapeshiftClientState.shouldHideHands(player.getId())) {
            ci.cancel();
        }
    }
}
