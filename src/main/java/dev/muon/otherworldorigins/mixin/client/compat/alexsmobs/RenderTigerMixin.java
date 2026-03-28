package dev.muon.otherworldorigins.mixin.client.compat.alexsmobs;

import com.github.alexthe666.alexsmobs.client.render.RenderTiger;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * {@link RenderTiger} replaces {@link net.minecraft.client.renderer.entity.LivingEntityRenderer#render}
 * instead of calling {@code super}, so {@link dev.muon.otherworldorigins.mixin.client.LivingEntityRendererMixin}'s
 * skip around {@code EntityRenderer#render} never runs and nametags still draw in GUI previews.
 */
@Mixin(value = RenderTiger.class, remap = false)
public class RenderTigerMixin {

    @WrapWithCondition(
            method = "render(Lcom/github/alexthe666/alexsmobs/entity/EntityTiger;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/alexthe666/alexsmobs/client/render/RenderTiger;renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private boolean otherworldorigins$skipNametagDuringShapeshiftPreview(
            RenderTiger instance,
            Entity entity,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        return !ShapeshiftRenderHelper.isRenderingShapeshiftBody();
    }
}
