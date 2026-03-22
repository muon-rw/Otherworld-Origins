package dev.muon.otherworldorigins.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.muon.otherworldorigins.client.shapeshift.FakeEntityCache;
import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftClientState;
import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Shadow
    public abstract <T extends Entity> EntityRenderer<? super T> getRenderer(T entity);

    @WrapOperation(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private <E extends Entity> void otherworldorigins$renderShapeshifted(
            EntityRenderer<? super E> originalRenderer,
            E entity,
            float yaw,
            float tickDelta,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            Operation<Void> original
    ) {
        if (!(entity instanceof Player player)) {
            original.call(originalRenderer, entity, yaw, tickDelta, poseStack, bufferSource, light);
            return;
        }

        ResourceLocation shapeshiftType = ShapeshiftClientState.getShapeshiftType(entity.getId());
        if (shapeshiftType == null) {
            original.call(originalRenderer, entity, yaw, tickDelta, poseStack, bufferSource, light);
            return;
        }

        Entity fakeEntity = FakeEntityCache.getOrCreate(entity.getId(), shapeshiftType);
        if (fakeEntity == null) {
            original.call(originalRenderer, entity, yaw, tickDelta, poseStack, bufferSource, light);
            return;
        }

        ShapeshiftRenderHelper.syncVisualState(entity, fakeEntity);

        EntityRenderer<?> shapeshiftRenderer = this.getRenderer(fakeEntity);
        if (shapeshiftRenderer == null) {
            original.call(originalRenderer, entity, yaw, tickDelta, poseStack, bufferSource, light);
            return;
        }

        ShapeshiftRenderHelper.setRenderingShapeshiftBody(true);
        try {
            renderDelegated(shapeshiftRenderer, fakeEntity, yaw, tickDelta, poseStack, bufferSource, light);
        } finally {
            ShapeshiftRenderHelper.setRenderingShapeshiftBody(false);
        }

        firePlayerNametag(originalRenderer, player, tickDelta, poseStack, bufferSource, light);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> void renderDelegated(
            EntityRenderer<?> renderer, T entity, float yaw, float tickDelta,
            PoseStack poseStack, MultiBufferSource bufferSource, int light
    ) {
        ((EntityRenderer<T>) renderer).render(entity, yaw, tickDelta, poseStack, bufferSource, light);
    }

    /**
     * Fires a RenderNameTagEvent for the real player and calls renderNameTag directly,
     * without rendering the player body. This lets MobHealthBar and LevelDisplayRenderer
     * see the real Player entity with correct name, health, and level data.
     */
    private static void firePlayerNametag(
            EntityRenderer<?> playerRenderer, Player player,
            float tickDelta, PoseStack poseStack, MultiBufferSource bufferSource, int light
    ) {
        EntityRendererAccessor accessor = (EntityRendererAccessor) playerRenderer;
        RenderNameTagEvent event = new RenderNameTagEvent(
                player, player.getDisplayName(), playerRenderer,
                poseStack, bufferSource, light, tickDelta
        );
        MinecraftForge.EVENT_BUS.post(event);
        if (event.getResult() != Event.Result.DENY
                && (event.getResult() == Event.Result.ALLOW || accessor.invokeShouldShowName(player))) {
            accessor.invokeRenderNameTag(player, event.getContent(), poseStack, bufferSource, light);
        }
    }
}
