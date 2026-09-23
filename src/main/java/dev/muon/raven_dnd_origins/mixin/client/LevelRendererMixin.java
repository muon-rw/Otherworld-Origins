package dev.muon.raven_dnd_origins.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftRenderHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @WrapMethod(method = "renderLevel")
    private void raven_dnd_origins$markRenderingLevel(
            DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
            LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, Operation<Void> original
    ) {
        ShapeshiftRenderHelper.setRenderingLevel(true);
        try {
            original.call(deltaTracker, renderBlockOutline, camera, gameRenderer, lightTexture, frustumMatrix, projectionMatrix);
        } finally {
            ShapeshiftRenderHelper.setRenderingLevel(false);
        }
    }
}
