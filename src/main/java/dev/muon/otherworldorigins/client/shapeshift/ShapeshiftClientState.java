package dev.muon.otherworldorigins.client.shapeshift;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side tracking of which players are shapeshifted and into what entity type,
 * along with per-player shapeshift flags (hide_hands, allow_tools).
 */
@OnlyIn(Dist.CLIENT)
public class ShapeshiftClientState {

    private static final Map<Integer, ShapeshiftData> ACTIVE_SHAPESHIFTS = new ConcurrentHashMap<>();

    public static void handleSync(int playerId, @Nullable ResourceLocation entityType,
                                  boolean hideHands, boolean allowTools) {
        if (entityType != null) {
            ACTIVE_SHAPESHIFTS.put(playerId, new ShapeshiftData(entityType, hideHands, allowTools));
        } else {
            ACTIVE_SHAPESHIFTS.remove(playerId);
            FakeEntityCache.evict(playerId);
            AnacondaMultipartHandler.evict(playerId);
            ShapeshiftRenderHelper.clearTracking(playerId);
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

    public static void clear() {
        ACTIVE_SHAPESHIFTS.clear();
        FakeEntityCache.clearAll();
        AnacondaMultipartHandler.clearAll();
        ShapeshiftRenderHelper.clearAllTracking();
    }

    private record ShapeshiftData(ResourceLocation entityType, boolean hideHands, boolean allowTools) {}
}
