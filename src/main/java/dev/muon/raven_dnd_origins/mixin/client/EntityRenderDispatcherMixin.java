package dev.muon.raven_dnd_origins.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.muon.raven_dnd_origins.client.shapeshift.FakeEntityCache;
import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftCameraObstruction;
import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftClientState;
import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import dev.muon.raven_dnd_origins.client.EagleFlightHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import java.util.HashMap;
import java.util.Map;

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
    private <E extends Entity> void raven_dnd_origins$renderShapeshifted(
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

        // playerAnimator's first-person body pass (Better Combat attacks, Iron's casts, and any
        // other FirstPersonMode.THIRD_PERSON_MODEL animation) renders the camera entity in first
        // person and relies on PlayerRenderer/PlayerModel part-hiding to show only the configured
        // arms/items. Delegating to the wildshape renderer bypasses that hiding and draws the full
        // mob over the camera. Fall back to the vanilla player renderer for that pass, matching the
        // hand policy in ItemInHandRendererMixin for hide_hands forms.
        if (FirstPersonMode.isFirstPersonPass() && player == Minecraft.getInstance().getCameraEntity()) {
            if (!ShapeshiftClientState.shouldHideHands(player.getId())) {
                original.call(originalRenderer, entity, yaw, tickDelta, poseStack, bufferSource, light);
            }
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

        float obstructionAlpha = ShapeshiftCameraObstruction.compute(player, fakeEntity, tickDelta);
        ShapeshiftRenderHelper.setRenderingShapeshiftBody(true);
        ShapeshiftRenderHelper.setShapeshiftBodyObstructionAlpha(obstructionAlpha);
        ShapeshiftRenderHelper.setUseTranslucentRenderTypes(obstructionAlpha < 1.0F);
        boolean tilted = raven_dnd_origins$pushFlightTilt(poseStack, player, fakeEntity, shapeshiftType, tickDelta);
        try {
            raven_dnd_origins$renderDelegated(shapeshiftRenderer, fakeEntity, yaw, tickDelta, poseStack, bufferSource, light);
        } finally {
            if (tilted) {
                poseStack.popPose();
            }
            ShapeshiftRenderHelper.setUseTranslucentRenderTypes(false);
            ShapeshiftRenderHelper.setShapeshiftBodyObstructionAlpha(1.0F);
            ShapeshiftRenderHelper.setRenderingShapeshiftBody(false);
        }

        raven_dnd_origins$firePlayerNametag(originalRenderer, player, tickDelta, poseStack, bufferSource, light);
    }

    @Unique
    private static final Map<Integer, Float> raven_dnd_origins$FLIGHT_BANK = new HashMap<>();
    @Unique
    private static final float raven_dnd_origins$BANK_SMOOTHING = 0.2F;

    /**
     * Flying mobs keep their body level and only turn the neck, so a fall-flying player would glide
     * as a horizontal plank. Pitch the whole form to the look angle and bank it into turns the way
     * vanilla does for elytra players, pivoting on the body centre. The frame comes from the entity
     * yaw rather than the look vector, so a vertical climb or dive keeps its orientation instead of
     * snapping level where the look vector loses its horizontal part.
     */
    @Unique
    private static boolean raven_dnd_origins$pushFlightTilt(PoseStack poseStack, Player player, Entity form,
                                                            ResourceLocation shapeshiftType, float tickDelta) {
        if (!player.isFallFlying() || !EagleFlightHandler.isPoweredFlightForm(player.getId(), shapeshiftType)) {
            raven_dnd_origins$FLIGHT_BANK.remove(player.getId());
            return false;
        }
        float yawRad = Mth.lerp(tickDelta, player.yRotO, player.getYRot()) * Mth.DEG_TO_RAD;
        float fx = -Mth.sin(yawRad);
        float fz = Mth.cos(yawRad);
        float pitch = Mth.lerp(tickDelta, player.xRotO, player.getXRot());
        float bank = raven_dnd_origins$smoothedBank(player, fx, fz);
        float pivot = form.getBbHeight() * 0.5F;
        poseStack.pushPose();
        poseStack.translate(0.0F, pivot, 0.0F);
        // +pitch about (up x facing) tips the nose down, matching a positive xRot
        poseStack.mulPose(new Quaternionf().setAngleAxis(pitch * Mth.DEG_TO_RAD, fz, 0.0F, -fx));
        poseStack.mulPose(new Quaternionf().setAngleAxis(bank, fx, 0.0F, fz));
        poseStack.translate(0.0F, -pivot, 0.0F);
        return true;
    }

    /** Vanilla's elytra bank (velocity against facing), eased per frame along the shorter arc. */
    @Unique
    private static float raven_dnd_origins$smoothedBank(Player player, float fx, float fz) {
        float last = raven_dnd_origins$FLIGHT_BANK.getOrDefault(player.getId(), 0.0F);
        Vec3 velocity = player.getDeltaMovement();
        double speedSq = velocity.horizontalDistanceSqr();
        float target = last;
        if (speedSq > 1.0e-6) {
            double speed = Math.sqrt(speedSq);
            double cos = (velocity.x * fx + velocity.z * fz) / speed;
            double cross = velocity.x * fz - velocity.z * fx;
            target = (float) (Math.signum(cross) * Math.acos(Mth.clamp(cos, -1.0, 1.0)));
        }
        float delta = Mth.wrapDegrees((target - last) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        float bank = last + delta * raven_dnd_origins$BANK_SMOOTHING;
        raven_dnd_origins$FLIGHT_BANK.put(player.getId(), bank);
        return bank;
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <T extends Entity> void raven_dnd_origins$renderDelegated(
            EntityRenderer<?> renderer, T entity, float yaw, float tickDelta,
            PoseStack poseStack, MultiBufferSource bufferSource, int light
    ) {
        ((EntityRenderer<T>) renderer).render(entity, yaw, tickDelta, poseStack, bufferSource, light);
    }

    /**
     * Replicates the nametag block from EntityRenderer.render() for the real player,
     * so that RenderNameTagEvent fires exactly once. Mods like HealthBars and
     * LevelDisplayRenderer respond to this event to render healthbars and levels.
     */
    @Unique
    private static void raven_dnd_origins$firePlayerNametag(
            EntityRenderer<?> playerRenderer, Player player,
            float tickDelta, PoseStack poseStack, MultiBufferSource bufferSource, int light
    ) {
        EntityRendererAccessor accessor = (EntityRendererAccessor) playerRenderer;
        RenderNameTagEvent event = new RenderNameTagEvent(
                player, player.getDisplayName(), playerRenderer,
                poseStack, bufferSource, light, tickDelta
        );
        NeoForge.EVENT_BUS.post(event);
        if (event.canRender().isTrue()
                || event.canRender().isDefault() && accessor.invokeShouldShowName(player)) {
            accessor.invokeRenderNameTag(player, event.getContent(), poseStack, bufferSource, light, tickDelta);
        }
    }
}
