package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.power.ActionOnSpellDamagePower;

/**
 * Suppresses {@link ActionOnSpellDamagePower} while a configured action runs, so nested
 * follow-up spell damage in {@link net.minecraft.world.entity.LivingEntity#actuallyHurt} does not re-enter.
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
