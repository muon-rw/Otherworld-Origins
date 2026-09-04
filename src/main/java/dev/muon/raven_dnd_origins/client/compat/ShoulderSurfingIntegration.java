package dev.muon.raven_dnd_origins.client.compat;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.world.phys.PickContext;
import com.github.exopandora.shouldersurfing.api.util.Couple;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftCollisionHelper;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftCollisionShape;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;

import java.util.function.Predicate;
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

    /**
     * Custom implementation of lookAtCrosshairTarget that ignores non-solid blocks.
     * Uses the same logic as Better Combat's "swing thru grass" feature:
     * blocks with empty collision shapes and blocks with 0 hardness (instantly breakable) are ignored.
     */
    public static void lookAtCrosshairTarget() {
        IShoulderSurfing instance = getShoulderSurfing();
        if (instance == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        var config = instance.getClientConfig();
        double interactionRange = config.getCrosshairConfig().getCrosshairType().isAimingDecoupled()
                ? 400.0
                : config.getObjectPickerConfig().getCustomRaytraceDistance();

        PickContext pickContext = new PickContext.Builder(camera).build();
        Couple<Vec3> blockTrace = pickContext.blockTrace(interactionRange, 1.0F);
        Vec3 startPos = blockTrace.left();
        Vec3 endPos = blockTrace.right();

        HitResult blockHit = clipIgnoringPassableBlocks(minecraft.level, startPos, endPos);

        double blockDist = blockHit.getType() != HitResult.Type.MISS
                ? startPos.distanceTo(blockHit.getLocation())
                : interactionRange;

        Couple<Vec3> entityTrace = pickContext.entityTrace(interactionRange, 1.0F);
        Vec3 entityStart = entityTrace.left();
        Vec3 entityEnd = entityTrace.right();

        Vec3 direction = entityEnd.subtract(entityStart).normalize();
        Vec3 scaledEntityEnd = entityStart.add(direction.scale(blockDist));

        AABB searchBox = player.getBoundingBox()
                .expandTowards(direction.scale(blockDist))
                .inflate(1.0);

        Predicate<Entity> entityFilter = e -> !e.isSpectator() && e.isPickable() && e != player;
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, entityStart, scaledEntityEnd, searchBox, entityFilter, blockDist * blockDist);

        Vec3 targetLocation;
        if (entityHit != null
                && (blockHit.getType() == HitResult.Type.MISS
                    || startPos.distanceTo(entityHit.getLocation()) < blockDist)) {
            targetLocation = entityHit.getLocation();
        } else {
            targetLocation = blockHit.getLocation();
        }

        lookAtTarget(player, targetLocation);
    }

    private static HitResult clipIgnoringPassableBlocks(BlockGetter level, Vec3 start, Vec3 end) {
        return BlockGetter.traverseBlocks(start, end, null, (context, blockPos) -> {
            BlockState blockState = level.getBlockState(blockPos);
            if (isPassableBlock(level, blockPos, blockState)) {
                return null;
            }
            VoxelShape shape = blockState.getCollisionShape(level, blockPos);
            if (shape.isEmpty()) {
                return null;
            }
            return shape.clip(start, end, blockPos);
        }, context -> {
            Vec3 direction = start.subtract(end);
            return BlockHitResult.miss(end,
                    Direction.getNearest(direction.x, direction.y, direction.z),
                    BlockPos.containing(end));
        });
    }

    /** Matches Better Combat's swing-through-grass logic: no collision shape, or instantly breakable. */
    private static boolean isPassableBlock(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return true;
        }
        return state.getDestroySpeed(level, pos) == 0.0F;
    }

    /**
     * Rotates the player toward the target and pushes the rotation to the server immediately, so the
     * server already has it when the following power activation packet arrives.
     */
    private static void lookAtTarget(LocalPlayer player, Vec3 target) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        player.setXRot((float) -Math.toDegrees(Math.atan2(dy, horizontalDist)));
        player.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));

        if (player.connection != null) {
            player.connection.send(new ServerboundMovePlayerPacket.Rot(
                    player.getYRot(),
                    player.getXRot(),
                    player.onGround()
            ));
        }
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
