package dev.muon.otherworldorigins.power;

import io.github.edwinmindcraft.apoli.api.configuration.HolderConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBlockCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.NonNullSupplier;

/**
 * Power that prevents block slowdown when the entity is in a block matching the block_condition.
 * Hooks into Entity.makeStuckInBlock - when the block condition matches, the slowdown is negated
 * (motion multiplier becomes 1,1,1 instead of the block's default).
 */
public class PreventBlockSlowdownPower extends PowerFactory<HolderConfiguration<ConfiguredBlockCondition<?, ?>>> {

    public PreventBlockSlowdownPower() {
        super(HolderConfiguration.required(ConfiguredBlockCondition.required("block_condition")));
    }

    /**
     * Returns true if the entity should not be slowed by the given block state.
     * Finds the block position by searching the entity's bounding box for the matching state.
     */
    public static boolean shouldPreventSlowdown(Entity entity, BlockState state) {
        if (entity == null) {
            return false;
        }
        // Find a block position with this state (entity may intersect multiple blocks)
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
        LevelReader level = entity.level();
        NonNullSupplier<BlockState> stateGetter = () -> state;
        return IPowerContainer.get(entity).resolve()
                .stream()
                .flatMap(container -> container.getPowers(ModPowers.PREVENT_BLOCK_SLOWDOWN.get()).stream())
                .anyMatch(holder -> {
                    ConfiguredPower<HolderConfiguration<ConfiguredBlockCondition<?, ?>>, ?> config = holder.value();
                    Holder<ConfiguredBlockCondition<?, ?>> blockCondition = config.getConfiguration().holder();
                    return ConfiguredBlockCondition.check(blockCondition, level, pos, stateGetter);
                });
    }
}
