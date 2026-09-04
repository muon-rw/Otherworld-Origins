package dev.muon.raven_dnd_origins.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftRenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Redirects opaque entity render types to their translucent equivalents while a wildshape
 * body is being drawn with obstruction fade. Without this, per-vertex alpha set by
 * {@link dev.muon.raven_dnd_origins.mixin.client.ModelPartShapeshiftObstructionMixin} is
 * silently ignored because {@code entityCutoutNoCull} does not enable GPU alpha blending.
 */
@Mixin(RenderType.class)
public class RenderTypeShapeshiftMixin {

    @ModifyReturnValue(
            method = "entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("RETURN")
    )
    private static RenderType raven_dnd_origins$translucentCutoutNoCull(RenderType original,
                                                                        ResourceLocation texture) {
        if (ShapeshiftRenderHelper.shouldUseTranslucentRenderTypes()) {
            return RenderType.entityTranslucent(texture);
        }
        return original;
    }

    @ModifyReturnValue(
            method = "entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("RETURN")
    )
    private static RenderType raven_dnd_origins$translucentCutoutNoCullOutline(RenderType original,
                                                                               ResourceLocation texture,
                                                                               boolean outline) {
        if (ShapeshiftRenderHelper.shouldUseTranslucentRenderTypes()) {
            return RenderType.entityTranslucent(texture, outline);
        }
        return original;
    }

    @ModifyReturnValue(
            method = "entityCutout(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("RETURN")
    )
    private static RenderType raven_dnd_origins$translucentCutout(RenderType original,
                                                                   ResourceLocation texture) {
        if (ShapeshiftRenderHelper.shouldUseTranslucentRenderTypes()) {
            return RenderType.entityTranslucentCull(texture);
        }
        return original;
    }

    @ModifyReturnValue(
            method = "entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("RETURN")
    )
    private static RenderType raven_dnd_origins$translucentSolid(RenderType original,
                                                                  ResourceLocation texture) {
        if (ShapeshiftRenderHelper.shouldUseTranslucentRenderTypes()) {
            return RenderType.entityTranslucentCull(texture);
        }
        return original;
    }
}
