package dev.muon.otherworldorigins.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Invoker("renderNameTag")
    void invokeRenderNameTag(Entity entity, Component displayName, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight);

    @Invoker("shouldShowName")
    boolean invokeShouldShowName(Entity entity);
}
