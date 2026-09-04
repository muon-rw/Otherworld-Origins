package dev.muon.raven_dnd_origins.effect;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Blade Flourish capstone (level 20): each flourish stack adds +5% attack speed for 5 seconds.
 * {@link net.minecraft.world.effect.MobEffect#createModifiers} multiplies the registered amount by
 * {@code (amplifier + 1)}, so a base {@code 0.05} {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL}
 * entry yields 5%, 10%, 15%, … for amplifiers 0, 1, 2, …
 */
public class FlourishMomentumEffect extends MobEffect {

    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID = RavenDndOrigins.loc("flourish_momentum_attack_speed");

    public FlourishMomentumEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE8D47A);
        this.addAttributeModifier(
                Attributes.ATTACK_SPEED,
                ATTACK_SPEED_MODIFIER_ID,
                0.05,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
