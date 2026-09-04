package dev.muon.raven_dnd_origins.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_core.leveling.LevelingUtils;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.world.entity.player.Player;

/**
 * Compares {@link LevelingUtils#getPlayerLevel} to {@code compare_to}. Non-players use {@code 0}.
 */
public final class PlayerLevelCondition implements ConditionType<EntityCtx, PlayerLevelCondition.Cfg> {
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
        int level = ctx.raw() instanceof Player player ? LevelingUtils.getPlayerLevel(player) : 0;
        return cfg.comparison().compare(level, cfg.compareTo());
    }
}
