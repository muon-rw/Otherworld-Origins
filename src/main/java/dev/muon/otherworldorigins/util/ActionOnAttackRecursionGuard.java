package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.power.ActionOnAttackPower;

/**
 * Suppresses {@link ActionOnAttackPower} while {@link dev.muon.otherworldorigins.action.bientity.AttackAction}
 * (or any other caller) runs a nested {@link net.minecraft.world.entity.player.Player#attack}.
 * Uses a per-thread depth counter so nested synthetic attacks stack correctly.
 */
public final class ActionOnAttackRecursionGuard {

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private ActionOnAttackRecursionGuard() {}

    /**
     * When {@code true}, {@link ActionOnAttackPower} should not run for this nested {@link net.minecraft.world.entity.player.Player#attack} call.
     */
    public static boolean isNestedPlayerAttack() {
        return DEPTH.get() > 0;
    }

    /**
     * Runs {@code runnable} (typically {@code player.attack}) with {@link ActionOnAttackPower} suppressed for inner events.
     */
    public static void runWithSuppressedActionOnAttack(Runnable runnable) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            runnable.run();
        } finally {
            int next = DEPTH.get() - 1;
            DEPTH.set(Math.max(0, next));
        }
    }
}
