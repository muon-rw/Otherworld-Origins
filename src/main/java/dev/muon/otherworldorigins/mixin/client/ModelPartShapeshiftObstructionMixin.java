package dev.muon.otherworldorigins.mixin.client;

import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftRenderHelper;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies {@link ShapeshiftRenderHelper#getShapeshiftBodyObstructionAlpha()} to delegated wildshape draws.
 * Shoulder Surfing only scales {@link ModelPart} alpha for the real player; fake mob renders must do the same.
 */
@Mixin(value = ModelPart.class, priority = 1100)
public class ModelPartShapeshiftObstructionMixin {

    @ModifyVariable(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 8
    )
    private float otherworldorigins$applyShapeshiftObstruction(float alpha) {
        if (!ShapeshiftRenderHelper.isRenderingShapeshiftBody()) {
            return alpha;
        }
        return Math.min(alpha, ShapeshiftRenderHelper.getShapeshiftBodyObstructionAlpha());
    }
}
