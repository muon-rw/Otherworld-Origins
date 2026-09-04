package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.EffectSpec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

/**
 * For each configured status effect template, applies that effect with duration and flags from the template.
 * If the entity already has the effect, the amplifier is set to {@code current + 1}; otherwise the template's
 * amplifier is used.
 */
public final class ApplyEffectStackingAction implements ActionType<EntityCtx, ApplyEffectStackingAction.Cfg> {

    public record Cfg(List<EffectSpec> effects) {}

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(
                RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
                        EffectSpec.LIST_OR_SINGLE.fieldOf("effect").forGetter(Cfg::effects)
                ).apply(i, Cfg::new)),
                Map.of("effects", "effect")
        );
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity living = ctx.living();
        if (living == null) {
            return;
        }
        for (EffectSpec spec : cfg.effects()) {
            MobEffectInstance template = spec.resolve(living);
            int amplifier = template.getAmplifier();
            if (living.hasEffect(template.getEffect())) {
                MobEffectInstance current = living.getEffect(template.getEffect());
                if (current != null) {
                    amplifier = current.getAmplifier() + 1;
                }
            }
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
