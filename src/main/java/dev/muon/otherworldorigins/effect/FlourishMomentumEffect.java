package dev.muon.otherworldorigins.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Blade Flourish capstone (level 20): each flourish stack adds +5% attack speed for 5 seconds.
 * {@link net.minecraft.world.effect.MobEffect#getAttributeModifierValue} multiplies the registered
 * amount by {@code (amplifier + 1)}, so a base {@code 0.05} {@link AttributeModifier.Operation#MULTIPLY_TOTAL}
 * entry yields 5%, 10%, 15%, … for amplifiers 0, 1, 2, …
 */
public class FlourishMomentumEffect extends MobEffect {

    private static final String ATTACK_SPEED_MODIFIER_UUID = "c4d8f1a2-6b3e-4c7d-9e0f-1a2b3c4d5e6f";

    public FlourishMomentumEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE8D47A);
        this.addAttributeModifier(
                Attributes.ATTACK_SPEED,
                ATTACK_SPEED_MODIFIER_UUID,
                0.05,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
