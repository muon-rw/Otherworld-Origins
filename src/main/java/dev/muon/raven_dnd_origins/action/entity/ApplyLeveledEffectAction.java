package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.LeveledScaling;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.EffectSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies one or more status effects to the acted-on entity. For each template, the final amplifier is
 * {@code template.amplifier + clamp(L - level_offset, min_amplifier, max_amplifier)} where {@code L} is
 * that entity's character level, or a Just Leveling aptitude level if {@code aptitude} is set.
 */
public final class ApplyLeveledEffectAction implements ActionType<EntityCtx, ApplyLeveledEffectAction.Cfg> {

    public record Cfg(
            List<EffectSpec> effects,
            int levelOffset,
            int minAmplifier,
            int maxAmplifier,
            Optional<ResourceLocation> aptitude
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(
                RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
                        EffectSpec.LIST_OR_SINGLE.fieldOf("effect").forGetter(Cfg::effects),
                        Codec.INT.optionalFieldOf("level_offset", 1).forGetter(Cfg::levelOffset),
                        Codec.INT.optionalFieldOf("min_amplifier", 0).forGetter(Cfg::minAmplifier),
                        Codec.INT.optionalFieldOf("max_amplifier", 19).forGetter(Cfg::maxAmplifier),
                        ResourceLocation.CODEC.optionalFieldOf("aptitude").forGetter(Cfg::aptitude)
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

        int level = LeveledScaling.levelForScaling(ctx.entity(), cfg.aptitude());
        int scaled = level - cfg.levelOffset();
        int leveledAmplifier = Mth.clamp(scaled, cfg.minAmplifier(), cfg.maxAmplifier());

        for (EffectSpec spec : cfg.effects()) {
            MobEffectInstance template = spec.resolve(living);
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
