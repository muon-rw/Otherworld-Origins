package dev.muon.otherworldorigins.condition.block;

import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BlockCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.NonNullSupplier;

/**
 * Block condition that matches blocks implementing {@link IPlantable} (e.g. farmland, dirt, grass).
 */
public class PlantableBlockCondition extends BlockCondition<NoConfiguration> {

    private static final BlockCondition.BlockPredicate PLANTABLE = (reader, pos, stateGetter) ->
            stateGetter.get().getBlock() instanceof IPlantable;

    public PlantableBlockCondition() {
        super(NoConfiguration.CODEC);
    }

    @Override
    protected boolean check(NoConfiguration configuration, LevelReader reader, BlockPos position, NonNullSupplier<BlockState> stateGetter) {
        return PLANTABLE.test(reader, position, stateGetter);
    }
}
