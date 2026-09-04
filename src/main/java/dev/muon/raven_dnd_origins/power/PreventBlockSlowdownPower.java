package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BlockCondition;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Prevents block slowdown when the entity is in a block matching the block_condition.
 * Hooks into Entity.makeStuckInBlock: when the block condition matches, the slowdown is negated
 * (motion multiplier becomes 1,1,1 instead of the block's default).
 */
public final class PreventBlockSlowdownPower extends PowerType<PreventBlockSlowdownPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCondition.CODEC.fieldOf("block_condition").forGetter(Configuration::blockCondition)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    /**
     * Returns true if the entity should not be slowed by the given block state.
     * Finds the block position by searching the entity's bounding box for the matching state.
     */
    public static boolean shouldPreventSlowdown(Entity entity, BlockState state) {
        if (entity == null) {
            return false;
        }
        BlockPos pos = BlockPos.betweenClosedStream(entity.getBoundingBox().deflate(1.0E-7D))
                .filter(p -> entity.level().getBlockState(p).equals(state))
                .findFirst()
                .orElse(entity.blockPosition());
        return shouldPreventSlowdown(entity, state, pos);
    }

    /**
     * Returns true if the entity should not be slowed by the given block state at the given position.
     */
    public static boolean shouldPreventSlowdown(Entity entity, BlockState state, BlockPos pos) {
        if (entity == null) {
            return false;
        }
        Level level = entity.level();
        BlockCtx ctx = new BlockCtx(pos, state, level);
        boolean[] prevents = new boolean[]{false};
        PowerLookup.forEach(entity, ModPowers.PREVENT_BLOCK_SLOWDOWN, Configuration.class, cfg -> {
            if (prevents[0]) return;
            if (cfg.blockCondition().test(ctx)) prevents[0] = true;
        });
        return prevents[0];
    }

    public record Configuration(BlockCondition blockCondition) {
    }
}
