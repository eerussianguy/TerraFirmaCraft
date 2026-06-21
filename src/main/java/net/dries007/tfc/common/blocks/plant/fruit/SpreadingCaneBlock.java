/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.plant.fruit;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;

public class SpreadingCaneBlock extends SpreadingBushBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape CANE_EAST = Block.box(0.0D, 3.0D, 0.0D, 8.0D, 12.0D, 16.0D);
    private static final VoxelShape CANE_WEST = Block.box(8.0D, 3.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    private static final VoxelShape CANE_SOUTH = Block.box(0.0D, 3.0D, 0.0D, 16.0D, 12.0D, 8.0D);
    private static final VoxelShape CANE_NORTH = Block.box(0.0D, 3.0D, 8.0D, 16.0D, 12.0D, 16.0D);

    public SpreadingCaneBlock(ExtendedProperties properties, Supplier<? extends Item> productItem, Lifecycle[] stages, Supplier<? extends Block> companion, int maxHeight, Supplier<ClimateRange> climateRange)
    {
        super(properties, productItem, stages, companion, maxHeight, climateRange);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return switch (state.getValue(FACING))
            {
                case NORTH -> CANE_NORTH;
                case WEST -> CANE_WEST;
                case SOUTH -> CANE_SOUTH;
                default -> CANE_EAST;
            };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    protected BlockState getDeadState(BlockState state)
    {
        return TFCBlocks.DEAD_CANE.get().defaultBlockState().setValue(STAGE, state.getValue(STAGE)).setValue(FACING, state.getValue(FACING));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        final BlockPos climatePos;
        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity plant)
        {
            climatePos = plant.getStemPos();
        }
        else
        {
            climatePos = pos;
        }
        final int hydration = getFruitBushHydrationFromRootPos(level, climatePos);
        final float temp = Climate.getAverageTemperature(level, climatePos);

        if (climateRange.get().checkBoth(hydration, temp, false))
        {
            this.tick(state, level, pos, random);
        }
        else
        {
            level.setBlockAndUpdate(pos, getDeadState(state));
        }
    }

    @Override
    protected void growAndPropagate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, int cycles, int growthsRemaining)
    {
        final int oldStage = state.getValue(STAGE);
        if (oldStage < 2)
        {
            final BlockState newState = state.setValue(STAGE, state.getValue(STAGE) + 1);
            placeBlockAndResetCounter(level, pos, newState, cycles, growthsRemaining);
            level.getBlockState(pos).randomTick(level, pos, level.random);
            return; // Increment stage if possible
        }

        // Otherwise, try and convert to a bush bock
        placeCompanionBlockAndResetCounter(level, pos, state, cycles, growthsRemaining);
    }

    private void placeCompanionBlockAndResetCounter(ServerLevel level, BlockPos pos, BlockState oldState, int cycles, int growths)
    {
        // Bush blocks start at stage = 1 when they're grown from another bush block, as stage = 0 is just for newly planted
        final BlockState placeState = companion.get().defaultBlockState().setValue(STAGE, 1).setValue(LIFECYCLE, oldState.getValue(LIFECYCLE));
        if (placeState.canSurvive(level, pos))
        {
            level.setBlock(pos, placeState, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity bush)
            {
                bush.resetCounter();
                bush.increaseCounter((long) TFCConfig.SERVER.berryBushGrowthTicks.get() * cycles);
                bush.setGrowthsRemaining(growths);

                bush.setStemPos(pos);
            }
            else
            {
                TerraFirmaCraft.LOGGER.error("Failed to update growing berry bush block entity at: {}", pos);
            }
            level.getBlockState(pos).tick(level, pos, level.random);
        }
    }

    @Override
    protected boolean mayPlaceOn(BlockState oldState, BlockGetter level, BlockPos pos)
    {
        return true;
    }

    @NotNull
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean canSurvive(BlockState oldState, LevelReader level, BlockPos pos)
    {
        return Helpers.isBlock(level.getBlockState(pos.relative(oldState.getValue(FACING).getOpposite())), TFCTags.Blocks.SPREADING_BUSHES);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState oldState, HitResult target, LevelReader level, BlockPos pos, Player player)
    {
        return new ItemStack(companion.get());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot)
    {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror)
    {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }
}
