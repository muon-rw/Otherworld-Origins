package dev.muon.raven_dnd_origins.client.compat;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import dev.muon.raven_core.compat.shouldersurfing.CrosshairTarget;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftCollisionHelper;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftCollisionShape;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftClientState;
import dev.muon.raven_dnd_origins.client.EagleFlightHandler;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;

/**
 * Client-side integration with Shoulder Surfing Reloaded.
 * Makes the player look at the crosshair target when casting spells.
 */
public class ShoulderSurfingIntegration {
    private static final String SHOULDER_SURFING_MOD_ID = "shouldersurfing";

    /** Vanilla {@link Player} standing footprint: {@code 0.6 + 1.8} blocks (width + height). */
    private static final float VANILLA_PLAYER_WIDTH = 0.6F;
    private static final float VANILLA_PLAYER_HEIGHT = 1.8F;
    private static final float VANILLA_PLAYER_COMBINED_SIZE = VANILLA_PLAYER_WIDTH + VANILLA_PLAYER_HEIGHT;

    /**
     * Maps combined-size ratio to offset scale using a softened curve ({@literal ratio^0.52}); clamp keeps
     * changes noticeable but sane for very large mobs.
     */
    private static final float SHAPESHIFT_ZOOM_CURVE_POWER = 0.52F;
    private static final float SHAPESHIFT_ZOOM_SCALE_MIN = 0.74F;
    private static final float SHAPESHIFT_ZOOM_SCALE_MAX = 2.65F;

    /** Forms whose model reaches well past the collision box (tails) or reads too small on the curve. */
    private record FormZoom(float extra, float min) {}

    private static final Map<ResourceLocation, FormZoom> FORM_ZOOM = Map.of(
            ResourceLocation.fromNamespaceAndPath("naturalist", "snake"), new FormZoom(1.4F, 1.18F),
            ResourceLocation.fromNamespaceAndPath("naturalist", "alligator"), new FormZoom(1.3F, 1.2F),
            ResourceLocation.fromNamespaceAndPath("naturalist", "bird"), new FormZoom(1.0F, 1.0F),
            ResourceLocation.fromNamespaceAndPath("luminous_beasts", "baby_phoenix"), new FormZoom(1.0F, 1.25F));

    /**
     * Shoulder Surfing orbits the eye position. Low forms put that under a block off the ground, so
     * the camera ends up level with the body and behind the tail; lift it to look down onto the form.
     */
    private static final float LOW_EYE_THRESHOLD = 1.2F;
    private static final float LOW_EYE_LIFT = 0.7F;

    /**
     * Powered flight tilts the form to the look angle, so a camera on the flight line sits behind
     * the tail. Raise it above the body and pull it back a little for an over-the-shoulder flight view.
     */
    private static final float FLIGHT_LIFT = 1.25F;
    private static final float FLIGHT_DISTANCE_MULTIPLIER = 1.15F;

    private static Boolean isShoulderSurfingLoaded = null;

    private static boolean isShoulderSurfingLoaded() {
        if (isShoulderSurfingLoaded == null) {
            isShoulderSurfingLoaded = ModList.get().isLoaded(SHOULDER_SURFING_MOD_ID);
        }
        return isShoulderSurfingLoaded;
    }

    private static IShoulderSurfing getShoulderSurfing() {
        if (!isShoulderSurfingLoaded()) {
            return null;
        }
        return IShoulderSurfing.getInstance();
    }

    private static boolean isShoulderSurfing() {
        IShoulderSurfing instance = getShoulderSurfing();
        return instance != null && instance.isShoulderSurfing();
    }

    public static void lookAtCrosshairTarget() {
        if (getShoulderSurfing() == null) {
            return;
        }
        CrosshairTarget.turnPlayer();
    }

    public static void lookAtCrosshairTargetIfShoulderSurfing() {
        if (isShoulderSurfing()) {
            lookAtCrosshairTarget();
        }
    }

    /**
     * After Shoulder Surfing applies its pose/modifier pipeline, scales the offset so larger forms
     * (especially wide ones, by combined width+height vs. the player) zoom out and smaller forms zoom in.
     */
    public static Vec3 scaleCameraOffsetForShapeshift(Vec3 targetOffset) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return targetOffset;
        }
        Entity cameraEntity = mc.getCameraEntity();
        if (!(cameraEntity instanceof Player player) || cameraEntity.getId() != mc.player.getId()) {
            return targetOffset;
        }
        ShapeshiftCollisionShape shape = ShapeshiftCollisionHelper.resolve(player);
        if (shape == null) {
            return targetOffset;
        }
        float sizeRatio = (shape.width() + shape.height()) / VANILLA_PLAYER_COMBINED_SIZE;
        float scale = Mth.clamp(
                (float) Math.pow(sizeRatio, SHAPESHIFT_ZOOM_CURVE_POWER),
                SHAPESHIFT_ZOOM_SCALE_MIN,
                SHAPESHIFT_ZOOM_SCALE_MAX);
        ResourceLocation form = ShapeshiftClientState.getShapeshiftType(player.getId());
        if (form == null) {
            // The collision shape resolves from the power before the client visual state is set
            ShapeshiftPower.Configuration cfg = ShapeshiftPower.getActiveShapeshiftConfig(player);
            form = cfg != null ? cfg.entityType() : null;
        }
        FormZoom formZoom = form != null ? FORM_ZOOM.get(form) : null;
        if (formZoom != null) {
            scale = Math.min(Math.max(scale, formZoom.min()) * formZoom.extra(), SHAPESHIFT_ZOOM_SCALE_MAX * 1.1F);
        }
        Vec3 scaled = targetOffset.scale(scale);
        if (form != null && player.isFallFlying() && EagleFlightHandler.isPoweredFlightForm(player.getId(), form)) {
            return new Vec3(scaled.x, Math.max(scaled.y, 0.0) + FLIGHT_LIFT, scaled.z * FLIGHT_DISTANCE_MULTIPLIER);
        }
        float lift = Mth.clamp(LOW_EYE_THRESHOLD - player.getEyeHeight(), 0.0F, 1.0F) * LOW_EYE_LIFT;
        return lift > 0.0F ? scaled.add(0.0, lift, 0.0) : scaled;
    }

    public static boolean shouldAimAtTarget() {
        return isCastingContinuousSpell();
    }

    /**
     * When Shoulder Surfing is present, reuse its "adjust player transparency" config for shapeshift obstruction.
     */
    public static boolean isShapeshiftObstructionFadeEnabled() {
        IShoulderSurfing instance = getShoulderSurfing();
        if (instance == null) {
            return true;
        }
        return instance.getClientConfig().getPlayerConfig().isPlayerTransparencyEnabled();
    }

    private static boolean isCastingContinuousSpell() {
        return ClientMagicData.isCasting() && ClientMagicData.getCastType() == CastType.CONTINUOUS;
    }
}
