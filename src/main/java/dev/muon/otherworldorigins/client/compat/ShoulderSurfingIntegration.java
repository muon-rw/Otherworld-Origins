package dev.muon.otherworldorigins.client.compat;

import com.github.exopandora.shouldersurfing.api.model.PickContext;
import com.github.exopandora.shouldersurfing.client.ObjectPicker;
import com.github.exopandora.shouldersurfing.client.ShoulderSurfingImpl;
import com.github.exopandora.shouldersurfing.config.Config;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fml.ModList;

import java.util.function.Predicate;

/**
 * Client-side integration with Shoulder Surfing Reloaded.
 * Makes the player look at the crosshair target when casting spells.
 */
public class ShoulderSurfingIntegration {
    private static final String SHOULDER_SURFING_MOD_ID = "shouldersurfing";
    private static Boolean isShoulderSurfingLoaded = null;

    private static boolean isShoulderSurfingLoaded() {
        if (isShoulderSurfingLoaded == null) {
            isShoulderSurfingLoaded = ModList.get().isLoaded(SHOULDER_SURFING_MOD_ID);
        }
        return isShoulderSurfingLoaded;
    }

    private static ShoulderSurfingImpl getShoulderSurfing() {
        if (!isShoulderSurfingLoaded()) {
            return null;
        }
        return ShoulderSurfingImpl.getInstance();
    }

    private static boolean isShoulderSurfing() {
        ShoulderSurfingImpl instance = getShoulderSurfing();
        return instance != null && instance.isShoulderSurfing();
    }

    /**
     * Custom implementation of lookAtCrosshairTarget that ignores non-solid blocks.
     * Uses the same logic as Better Combat's "swing thru grass" feature:
     * - Blocks with empty collision shapes are ignored
     * - Blocks with 0 hardness (instantly breakable) are ignored
     */
    public static void lookAtCrosshairTarget() {
        ShoulderSurfingImpl instance = getShoulderSurfing();
        if (instance == null) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        
        Camera camera = minecraft.gameRenderer.getMainCamera();
        double interactionRange = Config.CLIENT.getCrosshairType().isAimingDecoupled() 
                ? 400.0 
                : Config.CLIENT.getCustomRaytraceDistance();
        
        // Get the trace vectors from ShoulderSurfing's pick context
        PickContext pickContext = new PickContext.Builder(camera).build();
        var blockTrace = pickContext.blockTrace(interactionRange, 1.0F);
        Vec3 startPos = blockTrace.left();
        Vec3 endPos = blockTrace.right();
        
        // First, do a custom block raycast that ignores passable blocks
        HitResult blockHit = clipIgnoringPassableBlocks(minecraft.level, startPos, endPos, player);
        
        // Then check for entities
        double blockDist = blockHit.getType() != HitResult.Type.MISS 
                ? startPos.distanceTo(blockHit.getLocation()) 
                : interactionRange;
        
        var entityTrace = pickContext.entityTrace(interactionRange, 1.0F);
        Vec3 entityStart = entityTrace.left();
        Vec3 entityEnd = entityTrace.right();
        
        // Scale entity end position to block distance
        Vec3 direction = entityEnd.subtract(entityStart).normalize();
        Vec3 scaledEntityEnd = entityStart.add(direction.scale(blockDist));
        
        AABB searchBox = player.getBoundingBox()
                .expandTowards(direction.scale(blockDist))
                .inflate(1.0);
        
        Predicate<Entity> entityFilter = e -> !e.isSpectator() && e.isPickable() && e != player;
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, entityStart, scaledEntityEnd, searchBox, entityFilter, blockDist * blockDist);
        
        // Determine the final target location
        Vec3 targetLocation;
        if (entityHit != null) {
            double entityDist = startPos.distanceTo(entityHit.getLocation());
            if (blockHit.getType() == HitResult.Type.MISS || entityDist < blockDist) {
                targetLocation = entityHit.getLocation();
            } else {
                targetLocation = blockHit.getLocation();
            }
        } else {
            targetLocation = blockHit.getLocation();
        }
        
        // Make the player look at the target
        lookAtTarget(player, targetLocation);
        
        // Update ShoulderSurfing's camera state
        instance.getCamera().setLastMovedYRot(player.getYRot());
    }
    
    /**
     * Performs a block raycast that ignores "passable" blocks.
     * A block is considered passable if:
     * - Its collision shape is empty, OR
     * - Its hardness is 0 (instantly breakable, like grass)
     * 
     * This matches Better Combat's "swing thru grass" behavior.
     */
    private static HitResult clipIgnoringPassableBlocks(BlockGetter level, Vec3 start, Vec3 end, Entity entity) {
        // Use vanilla clip but with a custom block hit condition
        return BlockGetter.traverseBlocks(start, end, null, (context, blockPos) -> {
            BlockState blockState = level.getBlockState(blockPos);
            
            // Check if this block should be ignored (passable)
            if (isPassableBlock(level, blockPos, blockState)) {
                return null; // Continue through this block
            }
            
            // Check for actual collision
            VoxelShape shape = blockState.getCollisionShape(level, blockPos);
            if (shape.isEmpty()) {
                return null; // No collision shape to hit
            }
            
            BlockHitResult hitResult = shape.clip(start, end, blockPos);
            return hitResult;
        }, (context) -> {
            // Miss - return end position
            Vec3 direction = start.subtract(end);
            return BlockHitResult.miss(end, 
                    net.minecraft.core.Direction.getNearest(direction.x, direction.y, direction.z), 
                    BlockPos.containing(end));
        });
    }
    
    /**
     * Determines if a block should be considered "passable" for raycast purposes.
     * Matches Better Combat's logic for swing-through-grass.
     */
    private static boolean isPassableBlock(BlockGetter level, BlockPos pos, BlockState state) {
        // Empty collision shape = passable (like flowers, grass, etc.)
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return true;
        }
        // Zero hardness = instantly breakable = passable (like tall grass)
        if (state.getDestroySpeed(level, pos) == 0.0F) {
            return true;
        }
        return false;
    }
    
    /**
     * Makes the player look at the specified target position.
     * Similar to EntityHelper.lookAtTarget from ShoulderSurfing.
     * Also sends a rotation sync packet to the server to ensure the rotation
     * is applied before any subsequent power activation packets.
     */
    private static void lookAtTarget(LocalPlayer player, Vec3 target) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        float yRot = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float xRot = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));
        
        player.setXRot(xRot);
        player.setYRot(yRot);
        
        // Force sync rotation to server immediately so the server has correct
        // rotation when processing the subsequent power activation packet
        syncRotationToServer(player);
    }
    
    /**
     * Sends a rotation packet to the server to ensure the player's look direction
     * is synced before any power activation packets are processed.
     */
    private static void syncRotationToServer(LocalPlayer player) {
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

    public static boolean shouldAimAtTarget() {
        return isCastingContinuousSpell();
    }

    private static boolean isContinuousSpell(CastType castType) {
        return castType == CastType.CONTINUOUS;
    }

    private static boolean isCastingContinuousSpell() {
        return ClientMagicData.isCasting() && isContinuousSpell(ClientMagicData.getCastType());
    }
}
