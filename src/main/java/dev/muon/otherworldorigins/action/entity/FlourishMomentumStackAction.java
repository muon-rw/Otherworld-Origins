package dev.muon.otherworldorigins.action.entity;

import dev.muon.otherworldorigins.effect.ModEffects;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Increments {@link ModEffects#FLOURISH_MOMENTUM} amplifier on each call and refreshes duration to 5 seconds.
 */
public class FlourishMomentumStackAction extends EntityAction<NoConfiguration> {

    private static final int DURATION_TICKS = 5 * 20;

    public FlourishMomentumStackAction() {
        super(NoConfiguration.CODEC);
    }

    @Override
    public void execute(NoConfiguration configuration, Entity entity) {
        if (entity.level().isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }
        var effect = ModEffects.FLOURISH_MOMENTUM.get();
        int amplifier = 0;
        if (living.hasEffect(effect)) {
            MobEffectInstance current = living.getEffect(effect);
            if (current != null) {
                amplifier = current.getAmplifier() + 1;
            }
        }
        living.addEffect(new MobEffectInstance(effect, DURATION_TICKS, amplifier, false, true, true));
    }
}
