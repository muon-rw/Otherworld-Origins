package dev.muon.otherworldorigins.client.shapeshift;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Maintains per-player cached dummy entities used for shapeshifted rendering.
 * Each player gets their own dummy so concurrent renders don't stomp each other's state.
 */
@OnlyIn(Dist.CLIENT)
public class FakeEntityCache {

    private static final Map<Integer, CachedEntry> CACHE = new HashMap<>();

    @Nullable
    public static Entity getOrCreate(int playerId, ResourceLocation entityTypeId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        CachedEntry existing = CACHE.get(playerId);
        if (existing != null && existing.typeId.equals(entityTypeId) && existing.entity != null) {
            return existing.entity;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        if (type == null) return null;

        Entity entity = type.create(level);
        if (entity == null) return null;

        CACHE.put(playerId, new CachedEntry(entityTypeId, entity));
        return entity;
    }

    public static void evict(int playerId) {
        CACHE.remove(playerId);
    }

    public static void clearAll() {
        CACHE.clear();
    }

    private record CachedEntry(ResourceLocation typeId, Entity entity) {}
}
