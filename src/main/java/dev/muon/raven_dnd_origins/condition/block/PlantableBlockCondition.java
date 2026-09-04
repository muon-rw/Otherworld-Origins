package dev.muon.raven_dnd_origins.condition.block;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BlockCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class PlantableBlockCondition implements ConditionType<BlockCtx, EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public boolean test(EmptyCfg cfg, BlockCtx ctx) {
        BlockState state = ctx.state();
        // BushBlock stands in for Forge's IPlantable: crops, saplings, flowers, stems, nether wart.
        return state.getBlock() instanceof BushBlock
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS);
    }
}
