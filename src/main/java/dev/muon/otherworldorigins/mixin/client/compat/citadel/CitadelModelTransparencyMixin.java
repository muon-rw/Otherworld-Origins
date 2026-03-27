package dev.muon.otherworldorigins.mixin.client.compat.citadel;

import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Citadel {@link BasicModelPart} / {@link AdvancedModelBox} draws bypass vanilla {@link net.minecraft.client.model.geom.ModelPart};
 * apply the same shapeshift obstruction alpha as {@link ModelPartShapeshiftObstructionMixin}.
 */
@Mixin(value = {BasicModelPart.class, AdvancedModelBox.class}, remap = false)
public class CitadelModelTransparencyMixin {

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
