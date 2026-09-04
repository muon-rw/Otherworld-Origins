package dev.muon.raven_dnd_origins.mixin.client;

import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftRenderHelper;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies {@link ShapeshiftRenderHelper#getShapeshiftBodyObstructionAlpha()} to delegated wildshape draws.
 * Shoulder Surfing only scales the real player's tint; fake mob renders must do the same.
 * 1.21.1 packs the tint into the ARGB {@code color} argument, so only its alpha byte is lowered.
 */
@Mixin(value = ModelPart.class, priority = 1100)
public class ModelPartShapeshiftObstructionMixin {

    @ModifyVariable(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 5
    )
    private int raven_dnd_origins$applyShapeshiftObstruction(int color) {
        if (!ShapeshiftRenderHelper.isRenderingShapeshiftBody()) {
            return color;
        }
        int alpha = color >>> 24;
        int capped = Math.round(ShapeshiftRenderHelper.getShapeshiftBodyObstructionAlpha() * 255.0F);
        if (capped >= alpha) {
            return color;
        }
        return (capped << 24) | (color & 0x00FFFFFF);
    }
}
