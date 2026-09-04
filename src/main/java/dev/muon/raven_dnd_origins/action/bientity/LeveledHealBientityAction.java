package dev.muon.raven_dnd_origins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.LeveledScaling;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class LeveledHealBientityAction implements ActionType<BiEntityCtx, LeveledHealBientityAction.Cfg> {

    public record Cfg(
            float base,
            float perLevel,
            Optional<ResourceLocation> aptitude
    ) {}

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("base").forGetter(Cfg::base),
            Codec.FLOAT.fieldOf("per_level").forGetter(Cfg::perLevel),
            ResourceLocation.CODEC.optionalFieldOf("aptitude").forGetter(Cfg::aptitude)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        LivingEntity target = ctx.livingTarget();
        if (target == null || ctx.level().isClientSide()) return;

        int level = LeveledScaling.levelForScaling(ctx.actor(), cfg.aptitude());
        float healAmount = cfg.base() + (cfg.perLevel() * level);

        target.heal(healAmount);
    }
}
