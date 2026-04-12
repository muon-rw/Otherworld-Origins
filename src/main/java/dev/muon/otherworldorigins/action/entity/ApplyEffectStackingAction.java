package dev.muon.otherworldorigins.action.entity;

import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.apoli.api.configuration.ListConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * For each configured status effect template, applies that effect with duration and flags from the template.
 * If the entity already has the effect, the amplifier is set to {@code current + 1}; otherwise the template's
 * amplifier is used.
 */
public class ApplyEffectStackingAction extends EntityAction<ListConfiguration<MobEffectInstance>> {

    public ApplyEffectStackingAction() {
        super(ListConfiguration.codec(SerializableDataTypes.STATUS_EFFECT_INSTANCE, "effect", "effects"));
    }

    @Override
    public void execute(ListConfiguration<MobEffectInstance> configuration, Entity entity) {
        if (entity.level().isClientSide() || !(entity instanceof LivingEntity living)) {
            return;
        }
        for (MobEffectInstance template : configuration.getContent()) {
            MobEffect effect = template.getEffect();
            int duration = template.getDuration();
            int amplifier = template.getAmplifier();
            if (living.hasEffect(effect)) {
                MobEffectInstance current = living.getEffect(effect);
                if (current != null) {
                    amplifier = current.getAmplifier() + 1;
                }
            }
            living.addEffect(new MobEffectInstance(
                    effect,
                    duration,
                    amplifier,
                    template.isAmbient(),
                    template.isVisible(),
                    template.showIcon()));
        }
    }
}
