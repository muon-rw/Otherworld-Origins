package dev.muon.otherworldorigins.client.shapeshift;

import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityAnaconda;
import com.github.alexthe666.alexsmobs.entity.EntityAnacondaPart;
import com.github.alexthe666.alexsmobs.entity.util.AnacondaPartIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages client-side multipart body segments for the anaconda wildshape.
 * <p>
 * The real {@link EntityAnaconda} spawns 7 {@link EntityAnacondaPart} entities
 * server-side in its {@code tick()} method. Because the shapeshift system uses
 * a single fake entity that is never ticked, those parts never exist. This
 * handler creates fake parts on the client, maintains a yaw ring buffer to
 * drive the trailing-body effect, and repositions all segments each game tick
 * (including terrain-following height probes).
 */
@OnlyIn(Dist.CLIENT)
public class AnacondaMultipartHandler {

    private static final ResourceLocation ANACONDA_ID = ResourceLocation.fromNamespaceAndPath("alexsmobs", "anaconda");
    private static final int SEGMENT_COUNT = 7;
    private static final int HISTORY_SIZE = 256;

    private static final Map<Integer, MultipartData> CACHE = new HashMap<>();

    public static boolean isAnaconda(ResourceLocation entityTypeId) {
        return ANACONDA_ID.equals(entityTypeId);
    }

    @Nullable
    public static EntityAnacondaPart[] getParts(int playerId) {
        MultipartData data = CACHE.get(playerId);
        return data != null ? data.parts : null;
    }

    /**
     * Updates the ring buffer and repositions all body segments.
     * Gated by tick count so state only advances once per game tick,
     * even if called multiple times per frame.
     */
    public static void tickAndPosition(int playerId, Entity source, EntityAnaconda fakeHead) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        MultipartData data = CACHE.get(playerId);
        if (data == null) {
            data = createMultipartData(level);
            if (data == null) return;
            CACHE.put(playerId, data);
        }

        if (source.tickCount != data.prevTickCount) {
            data.prevTickCount = source.tickCount;
            updateHistory(data, source, level);
            repositionParts(data, source, fakeHead, level);
        }

        if (!Float.isNaN(data.lastSurfacePitch)) {
            fakeHead.setXRot(data.lastSurfacePitch);
            fakeHead.xRotO = Float.isNaN(data.lastSurfacePitchO) ? data.lastSurfacePitch : data.lastSurfacePitchO;
        }
    }

    // ---- position history ----

    private static void updateHistory(MultipartData data, Entity source, Level level) {
        Vec3 pos = source.position();
        float yaw = source.getYRot();
        float pitch = source.getXRot();

        if (data.historyCount == 0) {
            data.history[0] = new PosRot(pos, yaw, pitch);
            data.historyCount = 1;
            data.historyHead = 0;
            return;
        }

        PosRot last = data.history[data.historyHead];
        if (last.pos.distanceToSqr(pos) > 0.001) {
            data.historyHead = (data.historyHead + 1) % HISTORY_SIZE;
            data.history[data.historyHead] = new PosRot(pos, yaw, pitch);
            if (data.historyCount < HISTORY_SIZE) data.historyCount++;
        } else {
            data.history[data.historyHead] = new PosRot(last.pos, yaw, pitch);
        }

        // Apply gravitational influence to historical positions
        for (int i = 0; i < data.historyCount; i++) {
            if (i == data.historyHead) continue;
            PosRot pr = data.history[i];
            
            double low = getLowPartHeight(level, pr.pos.x, pr.pos.y, pr.pos.z);
            if (low < 0 && !isNextToWall(level, pr.pos.x, pr.pos.y, pr.pos.z)) {
                double fall = 0.15; // Fall speed per tick
                double newY = pr.pos.y - fall;
                if (low > -3.0 && newY < pr.pos.y + low) {
                    newY = pr.pos.y + low;
                }
                data.history[i] = new PosRot(new Vec3(pr.pos.x, newY, pr.pos.z), pr.yaw, pr.pitch);
            }
        }
    }

    private static PosRot getHistoryAtDistance(MultipartData data, double targetDist) {
        if (data.historyCount == 0) return null;
        if (targetDist <= 0) return data.history[data.historyHead];

        double accumulatedDist = 0;
        PosRot prev = data.history[data.historyHead];

        for (int i = 1; i < data.historyCount; i++) {
            int idx = (data.historyHead - i + HISTORY_SIZE) % HISTORY_SIZE;
            PosRot curr = data.history[idx];
            double dist = prev.pos.distanceTo(curr.pos);

            if (accumulatedDist + dist >= targetDist) {
                double excess = targetDist - accumulatedDist;
                double fraction = dist > 0 ? excess / dist : 0;
                Vec3 lerpedPos = prev.pos.lerp(curr.pos, fraction);
                float lerpedYaw = Mth.rotLerp((float)fraction, prev.yaw, curr.yaw);
                float lerpedPitch = Mth.rotLerp((float)fraction, prev.pitch, curr.pitch);
                return new PosRot(lerpedPos, lerpedYaw, lerpedPitch);
            }
            accumulatedDist += dist;
            prev = curr;
        }
        
        int oldestIdx = (data.historyHead - data.historyCount + 1 + HISTORY_SIZE) % HISTORY_SIZE;
        PosRot oldest = data.history[oldestIdx];
        double remainingDist = targetDist - accumulatedDist;
        
        float f = oldest.pitch * ((float)Math.PI / 180F);
        float f1 = -oldest.yaw * ((float)Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        Vec3 forward = new Vec3((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));
        
        Vec3 extrapolatedPos = oldest.pos.subtract(forward.scale(remainingDist));
        return new PosRot(extrapolatedPos, oldest.yaw, oldest.pitch);
    }


    // ---- repositioning ----

    private static Vec3 getRightVector(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vec3(-Math.cos(rad), 0, -Math.sin(rad));
    }

    private static void repositionParts(MultipartData data, Entity source,
                                        EntityAnaconda fakeHead, Level level) {
        float walkDist = (source instanceof LivingEntity living) ? living.walkDist : 0;
        float scale = fakeHead.getScale();

        data.lastSurfacePitchO = data.lastSurfacePitch;

        // Adjust head pitch based on actual movement
        Vec3 movement = source.position().subtract(new Vec3(source.xo, source.yo, source.zo));
        if (movement.lengthSqr() > 0.0005) {
            double horizLen = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
            float targetPitch = (float) (-(Mth.atan2(movement.y, horizLen) * 180.0F / Math.PI));
            
            if (Float.isNaN(data.lastSurfacePitch)) {
                data.lastSurfacePitch = targetPitch;
                data.lastSurfacePitchO = targetPitch;
            } else {
                data.lastSurfacePitch = smoothPitchToward(data.lastSurfacePitch, targetPitch, 15.0F);
            }
        }

        double accumulatedDist = AnacondaPartIndex.HEAD.getBackOffset() * scale;
        
        PosRot p0_path = getHistoryAtDistance(data, accumulatedDist);
        if (p0_path == null) p0_path = new PosRot(source.position(), source.getYRot(), source.getXRot());
        
        float latOffset0 = (float) (Math.sin(walkDist * 6.0F) * 0.15 * scale);
        Vec3 frontJoint = p0_path.pos.add(getRightVector(p0_path.yaw).scale(latOffset0));

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            EntityAnacondaPart part = data.parts[i];
            if (part == null) continue;

            part.xo = part.getX();
            part.yo = part.getY();
            part.zo = part.getZ();

            AnacondaPartIndex thisPartType = part.getPartType();
            
            double partLength = (thisPartType.getBackOffset() + 0.5F * part.getBbWidth()) * scale;
            accumulatedDist += partLength;

            PosRot p1_path = getHistoryAtDistance(data, accumulatedDist);
            if (p1_path == null) p1_path = new PosRot(source.position(), source.getYRot(), source.getXRot());

            float diminish1 = 1.0F - ((i + 1) / (float)SEGMENT_COUNT) * 0.6F;
            float latOffset1 = (float) (Math.sin(walkDist * 6.0F - (i + 1) * 2.0F) * 0.15 * scale * diminish1);
            Vec3 backJoint = p1_path.pos.add(getRightVector(p1_path.yaw).scale(latOffset1));

            Vec3 center = frontJoint.add(backJoint).scale(0.5);
            Vec3 dir = frontJoint.subtract(backJoint);
            
            float yaw, pitch;
            if (dir.lengthSqr() > 0.0001) {
                yaw = (float) (Mth.atan2(dir.z, dir.x) * 180.0F / Math.PI) - 90.0F;
                double horizLen = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
                // The body parts use an inverted pitch compared to the head
                pitch = (float) (Mth.atan2(dir.y, horizLen) * 180.0F / Math.PI);
            } else {
                yaw = p1_path.yaw;
                pitch = -p1_path.pitch; // Invert history pitch as well
            }

            // Restore a small amount of explicit yaw wiggle to match the head's animation,
            // diminishing as we go down the tail so it doesn't disconnect too much.
            float diminishYaw = 1.0F - (i / (float)SEGMENT_COUNT) * 0.8F;
            float undulationYaw = (float) (-Math.sin(walkDist * 6.0F - i * 2.0F) * 12.0 * diminishYaw);
            yaw += undulationYaw;

            part.setXRot(pitch);
            part.setYRot(yaw);
            part.yHeadRot = yaw;
            part.yHeadRotO = yaw;
            part.yBodyRot = yaw;
            part.yBodyRotO = yaw;
            part.setPosRaw(center.x, center.y, center.z);

            part.copyDataFrom(fakeHead);

            if (source instanceof LivingEntity livingSource) {
                part.hurtTime = livingSource.hurtTime;
                part.deathTime = livingSource.deathTime;
            }

            frontJoint = backJoint;
        }
    }

    // ---- math helpers ----

    /**
     * Smooths entity pitch degrees toward a target. Uses plain delta (not {@link Mth#wrapDegrees}
     * on the result) so values stay in Minecraft's XRot range (~[-90, 90]); the old 0–360 wrap
     * broke lerped pitch after the first tick and made the head appear fixed.
     */
    private static float smoothPitchToward(float current, float target, float maxDelta) {
        float delta = target - current;
        if (delta > maxDelta) {
            delta = maxDelta;
        } else if (delta < -maxDelta) {
            delta = -maxDelta;
        }
        return Mth.clamp(current + delta, -90.0F, 90.0F);
    }

    // ---- terrain height probes ----

    private static double getLowPartHeight(Level level, double x, double yIn, double z) {
        if (isFluidAt(level, x, yIn, z)) return 0.0;
        double checkAt = 0.0;
        while (checkAt > -3.0 && !isOpaqueBlockAt(level, x, yIn + checkAt, z)) {
            checkAt -= 0.2;
        }
        return checkAt;
    }

    private static double getHighPartHeight(Level level, double x, double yIn, double z) {
        if (isFluidAt(level, x, yIn, z)) return 0.0;
        double checkAt = 0.0;
        while (checkAt <= 3.0 && isOpaqueBlockAt(level, x, yIn + checkAt, z)) {
            checkAt += 0.2;
        }
        return checkAt;
    }

    private static boolean isOpaqueBlockAt(Level level, double x, double y, double z) {
        Vec3 vec3 = new Vec3(x, y, z);
        net.minecraft.world.phys.AABB aabb = net.minecraft.world.phys.AABB.ofSize(vec3, 1.0, 1.0E-6, 1.0);
        return level.getBlockStates(aabb)
                .filter(java.util.function.Predicate.not(net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase::isAir))
                .anyMatch(state -> {
                    net.minecraft.core.BlockPos blockpos = net.minecraft.core.BlockPos.containing(vec3);
                    return state.isSuffocating(level, blockpos)
                            && net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(
                            state.getCollisionShape(level, blockpos).move(vec3.x, vec3.y, vec3.z),
                            net.minecraft.world.phys.shapes.Shapes.create(aabb),
                            net.minecraft.world.phys.shapes.BooleanOp.AND);
                });
    }

    private static boolean isNextToWall(Level level, double x, double y, double z) {
        net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(x - 0.75, y, z - 0.75, x + 0.75, y + 0.5, z + 0.75);
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                Mth.floor(aabb.minX), Mth.floor(aabb.minY), Mth.floor(aabb.minZ),
                Mth.floor(aabb.maxX), Mth.floor(aabb.maxY), Mth.floor(aabb.maxZ))) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(level, pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFluidAt(Level level, double x, double y, double z) {
        return !level.getFluidState(net.minecraft.core.BlockPos.containing(x, y, z)).isEmpty();
    }

    // ---- cache management ----

    @Nullable
    @SuppressWarnings("unchecked")
    private static MultipartData createMultipartData(ClientLevel level) {
        EntityType<EntityAnacondaPart> partType =
                (EntityType<EntityAnacondaPart>) AMEntityRegistry.ANACONDA_PART.get();

        EntityAnacondaPart[] parts = new EntityAnacondaPart[SEGMENT_COUNT];
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            EntityAnacondaPart part = partType.create(level);
            if (part == null) return null;
            part.setBodyIndex(i);
            part.setPartType(AnacondaPartIndex.sizeAt(1 + i));
            parts[i] = part;
        }

        return new MultipartData(parts);
    }

    public static void evict(int playerId) {
        CACHE.remove(playerId);
    }

    public static void clearAll() {
        CACHE.clear();
    }

    private static class PosRot {
        final Vec3 pos;
        final float yaw;
        final float pitch;

        PosRot(Vec3 pos, float yaw, float pitch) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static class MultipartData {
        final EntityAnacondaPart[] parts;
        final PosRot[] history = new PosRot[HISTORY_SIZE];
        int historyHead = 0;
        int historyCount = 0;
        int prevTickCount = -1;
        final double[] prevHeights;
        float lastSurfacePitch = Float.NaN;
        float lastSurfacePitchO = Float.NaN;

        MultipartData(EntityAnacondaPart[] parts) {
            this.parts = parts;
            this.prevHeights = new double[SEGMENT_COUNT];
        }
    }
}
