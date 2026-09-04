package dev.muon.raven_dnd_origins.util;

import dev.muon.raven_dnd_origins.power.ActionOnSpellDamagePower;

/**
 * Suppresses {@link ActionOnSpellDamagePower} while a configured action runs, so nested
 * follow-up spell damage in {@code LivingEntity#actuallyHurt} does not re-enter.
 */
public final class ActionOnSpellDamageRecursionGuard {

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private ActionOnSpellDamageRecursionGuard() {
    }

    public static boolean isNestedSpellDamageAction() {
        return DEPTH.get() > 0;
    }

    public static void runNested(Runnable runnable) {
        int d = DEPTH.get();
        DEPTH.set(d + 1);
        try {
            runnable.run();
        } finally {
            DEPTH.set(d);
        }
    }
}
