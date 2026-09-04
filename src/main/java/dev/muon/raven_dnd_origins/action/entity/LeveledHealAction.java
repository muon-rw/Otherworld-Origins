package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.LeveledScaling;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class LeveledHealAction implements ActionType<EntityCtx, LeveledHealAction.Cfg> {

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
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity living = ctx.living();
        if (living == null) {
            return;
        }
        int level = LeveledScaling.levelForScaling(living, cfg.aptitude());
        float healAmount = cfg.base() + cfg.perLevel() * level;
        living.heal(healAmount);
    }
}
