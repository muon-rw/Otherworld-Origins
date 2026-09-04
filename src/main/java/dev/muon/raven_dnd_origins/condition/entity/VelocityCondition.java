package dev.muon.raven_dnd_origins.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;

public final class VelocityCondition implements ConditionType<EntityCtx, VelocityCondition.Cfg> {
    public record Cfg(Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
                Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        int velocityBps = (int) (ctx.raw().getDeltaMovement().length() * 20);
        return cfg.comparison().compare(velocityBps, cfg.compareTo());
    }
}
