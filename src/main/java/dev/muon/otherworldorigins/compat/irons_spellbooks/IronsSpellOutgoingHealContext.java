package dev.muon.otherworldorigins.compat.irons_spellbooks;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Bridges {@link io.redspace.ironsspellbooks.api.events.SpellHealEvent} to {@code apoli:modify_healing} on the
 * <em>caster</em>. Apoli only scales healing for the entity in {@link net.minecraftforge.event.entity.living.LivingHealEvent}
 * (the recipient); spell heals skip the caster's modifiers when the target is another entity.
 */
public final class IronsSpellOutgoingHealContext {

    private static final ThreadLocal<Deque<Pending>> PENDING = ThreadLocal.withInitial(ArrayDeque::new);

    private record Pending(LivingEntity target, float modifiedAmount) {}

    private IronsSpellOutgoingHealContext() {}

    public static void pushOutgoingHeal(LivingEntity target, float modifiedHealAmount) {
        PENDING.get().addLast(new Pending(target, modifiedHealAmount));
    }

    /**
     * If a pending Iron's spell heal targets {@code healed}, returns the caster-modified amount; otherwise the
     * original {@code healAmount}.
     */
    public static float consumeFor(LivingEntity healed, float healAmount) {
        Deque<Pending> q = PENDING.get();
        for (Iterator<Pending> it = q.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.target == healed) {
                it.remove();
                return p.modifiedAmount;
            }
        }
        return healAmount;
    }
}
