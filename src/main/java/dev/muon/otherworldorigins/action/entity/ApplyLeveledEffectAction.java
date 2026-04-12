package dev.muon.otherworldorigins.action.entity;

import dev.muon.otherworldorigins.action.bientity.ApplyLeveledEffectBientityAction;
import dev.muon.otherworldorigins.util.LeveledScaling;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Applies one or more status effects to the acted-on entity. For each template, the final amplifier is
 * {@code template.amplifier + clamp(L - level_offset, min_amplifier, max_amplifier)} where {@code L} is
 * that entity's character level, or a Just Leveling aptitude level if {@code aptitude} is set.
 */
public class ApplyLeveledEffectAction extends EntityAction<ApplyLeveledEffectBientityAction.Configuration> {

    public ApplyLeveledEffectAction() {
        super(ApplyLeveledEffectBientityAction.Configuration.CODEC);
    }

    @Override
    public void execute(ApplyLeveledEffectBientityAction.Configuration configuration, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity.level().isClientSide()) {
            return;
        }

        int level = LeveledScaling.levelForScaling(entity, configuration.aptitude());
        int scaled = level - configuration.levelOffset();
        int leveledAmplifier = Mth.clamp(scaled, configuration.minAmplifier(), configuration.maxAmplifier());

        for (MobEffectInstance template : configuration.effects().getContent()) {
            int amplifier = template.getAmplifier() + leveledAmplifier;
            living.addEffect(new MobEffectInstance(
                    template.getEffect(),
                    template.getDuration(),
                    amplifier,
                    template.isAmbient(),
                    template.isVisible(),
                    template.showIcon()));
        }
    }
}
