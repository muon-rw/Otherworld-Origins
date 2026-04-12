package dev.muon.otherworldorigins.util.spell;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side record of each living entity's most recently <em>completed</em> Iron's Spellbooks cast,
 * keyed by {@link UUID}. Populated from {@link io.redspace.ironsspellbooks.api.spells.AbstractSpell#onServerCastComplete}
 * via mixin after the real method runs.
 * <p>
 * Bounded by {@link #MAX_ENTRIES} and {@link #TTL_TICKS} (overworld game time). Uses only
 * {@link ConcurrentHashMap} bulk operations safe under concurrent readers/writers; no manual iterator
 * {@code remove()} during traversal.
 * <p>
 * Eviction is driven by TTL/trim, periodic server tick maintenance, player logout/death, non-player
 * {@link net.minecraftforge.event.entity.EntityLeaveLevelEvent} (players excluded so dimension travel
 * does not clear), and {@link net.minecraftforge.event.server.ServerStoppingEvent}.
 */
public final class RecentSpellCastCache {

    /**
     * How long a record remains valid, in overworld {@link Level#getGameTime()} ticks.
     */
    public static final long TTL_TICKS = 20 * 60 * 5;

    public static final int MAX_ENTRIES = 4096;

    private static final ConcurrentHashMap<UUID, Entry> MAP = new ConcurrentHashMap<>();

    private RecentSpellCastCache() {
    }

    private record Entry(ResourceLocation spellId, int level, long recordedAtGameTime) {
    }

    /**
     * @param cancelled when {@code true}, nothing is stored (interrupted / aborted cast).
     */
    public static void recordCompletedCast(Level level, LivingEntity caster, AbstractSpell spell, int spellLevel, boolean cancelled) {
        if (level.isClientSide || cancelled || spell == null || spell == SpellRegistry.none() || spellLevel < 1) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        long now = server.overworld().getGameTime();
        pruneExpired(now);
        MAP.put(caster.getUUID(), new Entry(spell.getSpellResource(), spellLevel, now));
        trimToMaxSize();
    }

    /**
     * Latest non-expired cast for this entity, resolved against the current spell registry.
     */
    public static Optional<ResolvedLastCast> getLastCast(LivingEntity entity, Level anyLevel) {
        if (anyLevel.isClientSide) {
            return Optional.empty();
        }
        MinecraftServer server = anyLevel.getServer();
        if (server == null) {
            return Optional.empty();
        }
        long now = server.overworld().getGameTime();
        long cutoff = now - TTL_TICKS;
        UUID id = entity.getUUID();
        Entry e = MAP.get(id);
        if (e == null || e.recordedAtGameTime < cutoff) {
            MAP.remove(id, e);
            return Optional.empty();
        }
        AbstractSpell resolved = SpellRegistry.getSpell(e.spellId);
        if (resolved == SpellRegistry.none()) {
            MAP.remove(id);
            return Optional.empty();
        }
        return Optional.of(new ResolvedLastCast(resolved, e.level));
    }

    public record ResolvedLastCast(AbstractSpell spell, int level) {
    }

    public static void remove(UUID entityId) {
        MAP.remove(entityId);
    }

    public static void clear() {
        MAP.clear();
    }

    /**
     * Drop expired rows; safe to call every few seconds from the server tick.
     */
    public static void maintenancePrune(@Nullable MinecraftServer server) {
        if (server == null) {
            return;
        }
        pruneExpired(server.overworld().getGameTime());
        trimToMaxSize();
    }

    private static void pruneExpired(long now) {
        long cutoff = now - TTL_TICKS;
        MAP.entrySet().removeIf(entry -> entry.getValue().recordedAtGameTime < cutoff);
    }

    private static void trimToMaxSize() {
        while (MAP.size() > MAX_ENTRIES) {
            UUID oldestKey = null;
            long oldestTime = Long.MAX_VALUE;
            for (Map.Entry<UUID, Entry> e : MAP.entrySet()) {
                long t = e.getValue().recordedAtGameTime;
                if (t < oldestTime) {
                    oldestTime = t;
                    oldestKey = e.getKey();
                }
            }
            if (oldestKey == null) {
                break;
            }
            MAP.remove(oldestKey);
        }
    }
}
