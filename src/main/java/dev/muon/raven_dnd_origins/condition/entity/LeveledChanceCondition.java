package dev.muon.raven_dnd_origins.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.LeveledScaling;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Optional;

public final class LeveledChanceCondition implements ConditionType<EntityCtx, LeveledChanceCondition.Cfg> {
    public record Cfg(float base, float perLevel, Optional<ResourceLocation> aptitude) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.fieldOf("base").forGetter(Cfg::base),
                Codec.FLOAT.fieldOf("per_level").forGetter(Cfg::perLevel),
                ResourceLocation.CODEC.optionalFieldOf("aptitude").forGetter(Cfg::aptitude)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!LeveledScaling.isValidAptitudeReference(cfg.aptitude())) {
            return false;
        }
        int level = LeveledScaling.levelForScaling(ctx.raw(), cfg.aptitude());
        float chance = cfg.base() + cfg.perLevel() * level;
        chance = Mth.clamp(chance, 0f, 1f);
        return ctx.level().getRandom().nextFloat() < chance;
    }
}
