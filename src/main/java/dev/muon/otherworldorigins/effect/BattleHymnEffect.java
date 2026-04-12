package dev.muon.otherworldorigins.effect;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/**
 * College of Valor Battle Hymn: buffs allies during Inspiration. Amplifier selects the combat tier
 * (set from bard level in datapack). Extra damage is applied in the damage pipeline — see
 * {@link dev.muon.otherworldorigins.ForgeEvents}. This effect also restores health over time.
 */
public class BattleHymnEffect extends MobEffect {

    public BattleHymnEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xC9A227);
    }

    /**
     * Damage multiplier for outgoing damage (1.0 = no change). {@code amplifier} is the effect amplifier (0–3).
     */
    public static float outgoingDamageMultiplier(int amplifier) {
        return switch (Mth.clamp(amplifier, 0, 3)) {
            case 3 -> 1.25f;
            case 2 -> 1.20f;
            case 1 -> 1.15f;
            default -> 1.10f;
        };
    }

    /**
     * Living entity responsible for dealing damage (melee/ranged attacker, or indirect owner when applicable).
     */
    public static LivingEntity resolveOutgoingDamageDealer(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof LivingEntity living) {
            return living;
        }
        Entity entity = source.getEntity();
        if (entity instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(0.5F + 0.5F * Mth.clamp(amplifier, 0, 3));
        }
    }
}
