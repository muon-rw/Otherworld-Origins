package dev.muon.otherworldorigins.client.shapeshift;

import com.github.exopandora.shouldersurfing.client.ShoulderSurfingImpl;
import dev.muon.otherworldorigins.client.compat.ShoulderSurfingIntegration;
import dev.muon.otherworldorigins.util.shapeshift.ShapeshiftCollisionShape;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;

/**
 * Camera obstruction fade for delegated shapeshift draws. Uses shoulder-surfing's renderOffset
 * (or an equivalent vanilla third-person offset) to compute how centred the camera is on the
 * wildshape silhouette, then produces an alpha in [{@value #MIN_CAMERA_ENTITY_ALPHA}, 1] that
 * the ModelPart mixins apply.
 */
@OnlyIn(Dist.CLIENT)
public final class ShapeshiftCameraObstruction {

    private static final String SHOULDER_SURFING_ID = "shouldersurfing";
    /** Minimum alpha floor. Note: values too far below this may render paradoxically opaque. */
    private static final float MIN_CAMERA_ENTITY_ALPHA = 0.15F;
    /**
     * Collision shapes for modded mobs generally underestimate visual model extent (tails, wings, heads, antlers, other shit sticking out for no reason)
     * Inflate bounds so the fade zone covers more of the actually rendered silhouette
     */
    private static final float VISUAL_EXTENT_MULTIPLIER = 1.15F;
    /** Higher = snappier tracking of the instantaneous target (~12 matches a few frames at 60 Hz). */
    private static final float SMOOTHING_RATE = 12.0F;

    private static float smoothedAlpha = 1.0F;
    private static long lastSmoothNanoTime;

    private ShapeshiftCameraObstruction() {}

    public static void resetSmoothing() {
        smoothedAlpha = 1.0F;
        lastSmoothNanoTime = 0L;
    }

    /**
     * @return blended vertex alpha cap in [{@value #MIN_CAMERA_ENTITY_ALPHA}, 1], smoothed over time
     */
    public static float compute(Player player, Entity fakeEntity, float partialTick) {
        float target = computeTargetAlpha(player, fakeEntity, partialTick);
        float k = smoothingStep();
        smoothedAlpha += (target - smoothedAlpha) * k;
        return Mth.clamp(smoothedAlpha, MIN_CAMERA_ENTITY_ALPHA, 1.0F);
    }

    private static float smoothingStep() {
        long now = System.nanoTime();
        if (lastSmoothNanoTime == 0L) {
            lastSmoothNanoTime = now;
            return 1.0F;
        }
        float dt = (now - lastSmoothNanoTime) * 1.0e-9F;
        lastSmoothNanoTime = now;
        dt = Mth.clamp(dt, 1.0e-4F, 0.08F);
        return 1.0F - (float) Math.exp(-SMOOTHING_RATE * dt);
    }

    private static float computeTargetAlpha(Player player, Entity fakeEntity, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != player.getId() || player.isSpectator()) {
            return 1.0F;
        }
        if (!ShoulderSurfingIntegration.isShapeshiftObstructionFadeEnabled()) {
            return 1.0F;
        }

        Bounds bounds = resolveBounds(player, fakeEntity);
        Vec3 offset = resolveCameraOffsetInBodySpace(player, partialTick);
        if (offset == null) {
            return 1.0F;
        }

        float halfW = bounds.width * 0.5F;
        float xNorm = halfW > 1.0e-4F ? (float) (Math.abs(offset.x) / halfW) : 0.0F;
        float xEff = Mth.clamp(xNorm, 0.0F, 1.0F);

        float yEff = verticalNorm(offset.y, bounds);

        float raw = Mth.sqrt(xEff * xEff + yEff * yEff);
        float norm = Mth.clamp(raw, 0.0F, 1.0F);
        float eased = smoothstep01(norm);
        return Mth.lerp(eased, MIN_CAMERA_ENTITY_ALPHA, 1.0F);
    }

    private static float smoothstep01(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private static float verticalNorm(double offsetY, Bounds b) {
        float denom;
        if (offsetY >= 0.0) {
            denom = Math.max(b.height - b.eyeHeight, 0.01F);
        } else {
            denom = Math.max(b.eyeHeight, 0.01F);
            offsetY = -offsetY;
        }
        return Mth.clamp((float) offsetY / denom, 0.0F, 1.0F);
    }

    private record Bounds(float width, float height, float eyeHeight) {}

    private static Bounds resolveBounds(Player player, Entity fake) {
        ShapeshiftCollisionShape shape = ShapeshiftClientState.getCollisionShape(player.getId());
        float rawW = shape != null ? shape.width() : fake.getBbWidth();
        float rawH = shape != null ? shape.height() : fake.getBbHeight();
        float w = rawW * VISUAL_EXTENT_MULTIPLIER;
        float h = rawH * VISUAL_EXTENT_MULTIPLIER;
        float eye;
        if (fake instanceof LivingEntity le && fake.getBbHeight() > 1.0e-4F) {
            eye = le.getEyeHeight() / fake.getBbHeight() * h;
        } else {
            eye = player.getEyeHeight() / player.getBbHeight() * h;
        }
        return new Bounds(w, h, Math.max(eye, 0.01F));
    }

    @Nullable
    private static Vec3 resolveCameraOffsetInBodySpace(Player player, float partialTick) {
        if (ModList.get().isLoaded(SHOULDER_SURFING_ID)) {
            ShoulderSurfingImpl inst = ShoulderSurfingImpl.getInstance();
            if (inst != null && inst.isShoulderSurfing()) {
                return inst.getCamera().getRenderOffset();
            }
        }
        return vanillaDetachedCameraOffset(player, partialTick);
    }

    @Nullable
    private static Vec3 vanillaDetachedCameraOffset(Player player, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized() || !camera.isDetached()) {
            return null;
        }
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 world = camera.getPosition().subtract(eye);
        float yRad = -player.getYRot() * Mth.DEG_TO_RAD;
        float c = Mth.cos(yRad);
        float s = Mth.sin(yRad);
        double x = world.x * (double) c - world.z * (double) s;
        double z = world.x * (double) s + world.z * (double) c;
        return new Vec3(x, world.y, z);
    }
}
