package dev.muon.raven_dnd_origins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.LeveledScaling;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.EffectSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies one or more status effects to the target. For each template, the final amplifier is
 * {@code template.amplifier + clamp(L - level_offset, min_amplifier, max_amplifier)} where {@code L} is
 * the actor's character level, or a Just Leveling aptitude level if {@code aptitude} is set.
 */
public final class ApplyLeveledEffectBientityAction implements ActionType<BiEntityCtx, ApplyLeveledEffectBientityAction.Cfg> {

    public record Cfg(
            List<EffectSpec> effects,
            int levelOffset,
            int minAmplifier,
            int maxAmplifier,
            Optional<ResourceLocation> aptitude
    ) {}

    private static final MapCodec<Cfg> CODEC = AliasingMapCodec.wrap(
            RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
                    EffectSpec.LIST_OR_SINGLE.fieldOf("effect").forGetter(Cfg::effects),
                    Codec.INT.optionalFieldOf("level_offset", 1).forGetter(Cfg::levelOffset),
                    Codec.INT.optionalFieldOf("min_amplifier", 0).forGetter(Cfg::minAmplifier),
                    Codec.INT.optionalFieldOf("max_amplifier", 19).forGetter(Cfg::maxAmplifier),
                    ResourceLocation.CODEC.optionalFieldOf("aptitude").forGetter(Cfg::aptitude)
            ).apply(i, Cfg::new)).validate(ApplyLeveledEffectBientityAction::validate),
            Map.of("effects", "effect"));

    private static DataResult<Cfg> validate(Cfg cfg) {
        if (cfg.effects().isEmpty()) {
            return DataResult.error(() -> "'effect' must contain at least one status effect");
        }
        if (!LeveledScaling.isValidAptitudeReference(cfg.aptitude())) {
            return DataResult.error(() -> "Unknown aptitude: " + cfg.aptitude().orElseThrow());
        }
        return DataResult.success(cfg);
    }

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        LivingEntity target = ctx.livingTarget();
        if (target == null || ctx.level().isClientSide()) return;

        int actorLevel = LeveledScaling.levelForScaling(ctx.actor(), cfg.aptitude());
        int scaled = actorLevel - cfg.levelOffset();
        int leveledAmplifier = Mth.clamp(scaled, cfg.minAmplifier(), cfg.maxAmplifier());

        for (EffectSpec spec : cfg.effects()) {
            int amplifier = spec.amplifier().evalInt(target) + leveledAmplifier;
            target.addEffect(new MobEffectInstance(
                    spec.effect(),
                    spec.duration().evalInt(target),
                    amplifier,
                    spec.ambient(),
                    spec.showParticles(),
                    spec.showIcon()));
        }
    }
}
