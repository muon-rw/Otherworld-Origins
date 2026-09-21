package dev.muon.raven_dnd_origins.util;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Immersive Melodies keeps no server state for free play or MIDI device input,
 * so notes relayed through its broadcast packet are recorded here instead.
 */
public final class LivePerformanceTracker {

    private static final long NOTE_LINGER_TICKS = 40L;

    private static final Map<LivingEntity, Performance> PERFORMANCES = new WeakHashMap<>();

    private LivePerformanceTracker() {
    }

    public static void onNote(LivingEntity musician, int tone, int velocity) {
        Performance performance = PERFORMANCES.computeIfAbsent(musician, ignored -> new Performance());
        if (velocity > 0) {
            performance.heldTones.add(tone);
            performance.lastNoteOnTime = musician.level().getGameTime();
        } else {
            performance.heldTones.remove(tone);
        }
    }

    public static boolean isPerforming(LivingEntity musician) {
        Performance performance = PERFORMANCES.get(musician);
        if (performance == null) {
            return false;
        }
        return !performance.heldTones.isEmpty()
                || musician.level().getGameTime() - performance.lastNoteOnTime <= NOTE_LINGER_TICKS;
    }

    public static void stop(LivingEntity musician) {
        PERFORMANCES.remove(musician);
    }

    private static final class Performance {
        private final Set<Integer> heldTones = new HashSet<>();
        private long lastNoteOnTime = Long.MIN_VALUE / 2;
    }
}
