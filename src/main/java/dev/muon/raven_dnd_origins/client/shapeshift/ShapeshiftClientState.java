package dev.muon.raven_dnd_origins.client.shapeshift;

import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftCollisionShape;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side tracking of which players are shapeshifted and into what entity type,
 * along with per-player shapeshift flags (hide_hands, allow_tools).
 */
public class ShapeshiftClientState {

    private static final Map<Integer, ShapeshiftData> ACTIVE_SHAPESHIFTS = new ConcurrentHashMap<>();

    public static void handleSync(int playerId, @Nullable ResourceLocation entityType,
                                  boolean hideHands, boolean allowTools,
                                  float collisionWidth, float collisionHeight, float flightSpeed) {
        if (entityType != null) {
            ShapeshiftCollisionShape collision =
                    collisionWidth > 0.0F && collisionHeight > 0.0F
                            ? new ShapeshiftCollisionShape(collisionWidth, collisionHeight)
                            : null;
            ACTIVE_SHAPESHIFTS.put(playerId, new ShapeshiftData(entityType, hideHands, allowTools, collision, flightSpeed));
        } else {
            ACTIVE_SHAPESHIFTS.remove(playerId);
            FakeEntityCache.evict(playerId);
            ShapeshiftRenderHelper.clearTracking(playerId);
            ShapeshiftCameraObstruction.resetSmoothing();
        }
    }

    @Nullable
    public static ResourceLocation getShapeshiftType(int entityId) {
        ShapeshiftData data = ACTIVE_SHAPESHIFTS.get(entityId);
        return data != null ? data.entityType : null;
    }

    public static boolean isShapeshifted(int entityId) {
        return ACTIVE_SHAPESHIFTS.containsKey(entityId);
    }

    public static boolean shouldHideHands(int entityId) {
        ShapeshiftData data = ACTIVE_SHAPESHIFTS.get(entityId);
        return data != null && data.hideHands;
    }

    public static boolean allowsTools(int entityId) {
        ShapeshiftData data = ACTIVE_SHAPESHIFTS.get(entityId);
        return data == null || data.allowTools;
    }

    @Nullable
    public static ShapeshiftCollisionShape getCollisionShape(int entityId) {
        ShapeshiftData data = ACTIVE_SHAPESHIFTS.get(entityId);
        return data != null ? data.collisionShape : null;
    }

    /** Multiplier on powered-flight thrust, 1.0 when unknown. */
    public static float flightSpeed(int entityId) {
        ShapeshiftData data = ACTIVE_SHAPESHIFTS.get(entityId);
        return data != null ? data.flightSpeed : 1.0F;
    }

    public static void clear() {
        ACTIVE_SHAPESHIFTS.clear();
        FakeEntityCache.clearAll();
        ShapeshiftRenderHelper.clearAllTracking();
        ShapeshiftCameraObstruction.resetSmoothing();
    }

    private record ShapeshiftData(
            ResourceLocation entityType,
            boolean hideHands,
            boolean allowTools,
            @Nullable ShapeshiftCollisionShape collisionShape,
            float flightSpeed
    ) {}
}
