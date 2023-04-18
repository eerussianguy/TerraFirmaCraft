package net.dries007.tfc.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface Rotator
{
    boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face);

    Direction.Axis getRotationAxis(BlockState state);
}
