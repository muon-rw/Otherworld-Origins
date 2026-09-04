package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.ExperimentalElixirLogic;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;

import java.util.Locale;
import java.util.Optional;

public final class ExperimentalElixirBrewAction implements ActionType<EntityCtx, ExperimentalElixirBrewAction.Cfg> {

    public record Cfg(MobEffectCategory category,
                      Optional<Integer> amplifierMin, Optional<Integer> amplifierMax,
                      Optional<Integer> durationMin, Optional<Integer> durationMax,
                      boolean lingering,
                      Optional<Integer> effectCountMin, Optional<Integer> effectCountMax) {
    }

    private static final Codec<MobEffectCategory> CATEGORY_CODEC = Codec.STRING.comapFlatMap(
            s -> switch (s.toLowerCase(Locale.ROOT)) {
                case "beneficial" -> DataResult.success(MobEffectCategory.BENEFICIAL);
                case "harmful" -> DataResult.success(MobEffectCategory.HARMFUL);
                case "neutral" -> DataResult.success(MobEffectCategory.NEUTRAL);
                default -> DataResult.error(() -> "Unknown effect category '" + s + "'; expected beneficial, harmful, or neutral");
            },
            c -> c.name().toLowerCase(Locale.ROOT));

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.<Cfg>mapCodec(i -> i.group(
            CATEGORY_CODEC.fieldOf("category").forGetter(Cfg::category),
            Codec.INT.optionalFieldOf("amplifier_min").forGetter(Cfg::amplifierMin),
            Codec.INT.optionalFieldOf("amplifier_max").forGetter(Cfg::amplifierMax),
            Codec.INT.optionalFieldOf("duration_min").forGetter(Cfg::durationMin),
            Codec.INT.optionalFieldOf("duration_max").forGetter(Cfg::durationMax),
            Codec.BOOL.optionalFieldOf("lingering", false).forGetter(Cfg::lingering),
            Codec.INT.optionalFieldOf("effect_count_min").forGetter(Cfg::effectCountMin),
            Codec.INT.optionalFieldOf("effect_count_max").forGetter(Cfg::effectCountMax)
    ).apply(i, Cfg::new)).validate(ExperimentalElixirBrewAction::validate);

    private static DataResult<Cfg> validate(Cfg cfg) {
        if (cfg.amplifierMin().isPresent() != cfg.amplifierMax().isPresent()) {
            return DataResult.error(() -> "amplifier_min and amplifier_max must be given together");
        }
        if (cfg.amplifierMin().isPresent() && cfg.amplifierMin().get() > cfg.amplifierMax().get()) {
            return DataResult.error(() -> "amplifier_min exceeds amplifier_max");
        }
        if (cfg.durationMin().isPresent() != cfg.durationMax().isPresent()) {
            return DataResult.error(() -> "duration_min and duration_max must be given together");
        }
        if (cfg.durationMin().isPresent() && cfg.durationMin().get() > cfg.durationMax().get()) {
            return DataResult.error(() -> "duration_min exceeds duration_max");
        }
        if (cfg.effectCountMin().isPresent() != cfg.effectCountMax().isPresent()) {
            return DataResult.error(() -> "effect_count_min and effect_count_max must be given together");
        }
        if (cfg.effectCountMin().isPresent()
                && (cfg.effectCountMin().get() < 1 || cfg.effectCountMin().get() > cfg.effectCountMax().get())) {
            return DataResult.error(() -> "effect_count_min must be at least 1 and no more than effect_count_max");
        }
        return DataResult.success(cfg);
    }

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (ctx.level().isClientSide() || !(ctx.entity() instanceof ServerPlayer player)) {
            return;
        }
        ExperimentalElixirLogic.brewForPlayer(player, cfg, player.getRandom());
    }
}
