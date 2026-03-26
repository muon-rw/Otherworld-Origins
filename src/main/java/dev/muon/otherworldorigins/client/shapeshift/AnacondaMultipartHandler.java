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

    private static final ResourceLocation ANACONDA_ID = new ResourceLocation("alexsmobs", "anaconda");
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
            updateHistory(data, source);
            repositionParts(data, source, fakeHead, level);
        }

        if (!Float.isNaN(data.lastSurfacePitch)) {
            fakeHead.setXRot(data.lastSurfacePitch);
            // We don't touch xRotO here because we want it to lerp from the previous tick's lastSurfacePitch
            // But wait, syncVisualState overwrites xRotO too.
            // Let's just force both to lastSurfacePitch for now, or let it be.
            fakeHead.xRotO = data.lastSurfacePitch;
        }
    }

    // ---- position history ----

    private static void updateHistory(MultipartData data, Entity source) {
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

    // ---- part rotation / undulation ----

    /**
     * Port of {@code EntityAnaconda.calcPartRotation(int)} with
     * strangleProgress/strangleTimer = 0 (player never strangles).
     */
    private static float calcPartRotation(float walkDist, int i) {
        return (float) (40.0 * -Math.sin(walkDist * 3.0F - i));
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

        // Smooth movement vector for head pitch over the last 0.5 blocks of travel
        PosRot pastForHead = getHistoryAtDistance(data, 0.5);
        if (pastForHead != null) {
            Vec3 headDir = source.position().subtract(pastForHead.pos);
            if (headDir.lengthSqr() > 0.005) {
                double horizLen = Math.sqrt(headDir.x * headDir.x + headDir.z * headDir.z);
                float targetPitch = (float) (-(Mth.atan2(headDir.y, horizLen) * 180.0F / Math.PI));
                
                if (Float.isNaN(data.lastSurfacePitch)) {
                    data.lastSurfacePitch = targetPitch;
                    data.lastSurfacePitchO = targetPitch;
                } else {
                    data.lastSurfacePitch = limitAngle(data.lastSurfacePitch, targetPitch, 15.0F);
                }
            }
        }

        double accumulatedDist = AnacondaPartIndex.HEAD.getBackOffset() * scale;
        
        PosRot p0_path = getHistoryAtDistance(data, accumulatedDist);
        if (p0_path == null) p0_path = new PosRot(source.position(), source.getYRot(), source.getXRot());
        
        float latOffset0 = (float) (Math.sin(walkDist * 3.0F) * 0.1 * scale);
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

            float latOffset1 = (float) (Math.sin(walkDist * 3.0F - (i + 1)) * 0.1 * scale);
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

    private static float limitAngle(float sourceAngle, float targetAngle, float maximumChange) {
        float f = Mth.wrapDegrees(targetAngle - sourceAngle);
        if (f > maximumChange) f = maximumChange;
        if (f < -maximumChange) f = -maximumChange;
        float f1 = sourceAngle + f;
        if (f1 < 0.0F) f1 += 360.0F;
        else if (f1 > 360.0F) f1 -= 360.0F;
        return f1;
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
