package dev.muon.otherworldorigins.effect;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/**
 * College of Lore Cutting Words: a harmful marker effect. Amplifier selects the damage tier
 * (set from bard level in datapack). Actual reduction is applied in the damage pipeline — see
 * {@link dev.muon.otherworldorigins.ForgeEvents}.
 */
public class CuttingWordsEffect extends MobEffect {

    public CuttingWordsEffect() {
        super(MobEffectCategory.HARMFUL, 0x7B68A6);
    }

    /**
     * Damage multiplier for outgoing damage (1.0 = no change). {@code amplifier} is the effect amplifier (0–3).
     */
    public static float outgoingDamageMultiplier(int amplifier) {
        return switch (Mth.clamp(amplifier, 0, 3)) {
            case 3 -> 0.75f;
            case 2 -> 0.80f;
            case 1 -> 0.85f;
            default -> 0.90f;
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
}
