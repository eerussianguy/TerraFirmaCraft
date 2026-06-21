/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.devices;

import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.DirectionPropertyBlock;
import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedBlock;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.util.ChannelFlow;
import net.dries007.tfc.util.Helpers;

public class ChannelBlock extends ExtendedBlock implements EntityBlockExtension
{
    public static final VoxelShape CHANNEL_FLOW_CENTER_SHAPE = Block.box(6.0f, 1.0f, 6.0f, 10.0f, 4.0f, 10.0f);
    public static final VoxelShape CHANNEL_FLOW_UP_SHAPE = Block.box(6.0f, 4.0f, 6.0f, 10.0f, 16.0f, 10.0f);
    public static final VoxelShape CHANNEL_FLOW_EAST_SHAPE = Block.box(10.0f, 1.0f, 6.0f, 17.0f, 4.0f, 10.0f);
    public static final VoxelShape CHANNEL_FLOW_EAST_SHAPE_LONG = Block.box(10.0f, 1.0f, 6.0f, 22.0f, 4.0f, 10.0f);
    public static final VoxelShape CHANNEL_FLOW_LONG_FALL_SHAPE = Block.box(6.0f, 0.0f, 6.0f, 10.0f, 16.0f, 10.0f);

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
        .entrySet().stream()
        .filter(facing -> facing.getKey() != Direction.UP).collect(Util.toMap());

    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty DOWN = PipeBlock.DOWN;

    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    public static final BooleanProperty WITH_METAL = BooleanProperty.create("with_metal");

    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static
    {
        final VoxelShape base = box(5.0D, 0.0D, 5.0D, 11.0D, 5.0D, 11.0D);

        final VoxelShape east = box(11.0D, 0.0D, 5.0D, 16.0D, 5.0D, 11.0D);
        final VoxelShape south = box(5.0D, 0.0D, 11.0D, 11.0D, 5.0D, 16.0D);
        final VoxelShape west = box(0.0D, 0.0D, 5.0D, 5.0D, 5.0D, 11.0D);
        final VoxelShape north = box(5.0D, 0.0D, 0.0D, 11.0D, 5.0D, 10.0D);

        final VoxelShape[] directions = new VoxelShape[] {south, west, north, east};

        for (int i = 0; i < SHAPES.length; i++)
        {
            VoxelShape shape = base;
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                if (((i >> direction.get2DDataValue()) & 1) == 1)
                {
                    shape = Shapes.or(shape, directions[direction.get2DDataValue()]);
                }
            }
            SHAPES[i] = shape;
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid)
    {
        // On destroy, notify source channel that all flows going through this
        // channel have been broken
        if (!level.isClientSide())
        {
            level.getBlockEntity(pos, TFCBlockEntities.CHANNEL.get()).ifPresent(
                channel -> channel.notifyBrokenLink(channel.getNumberOfFlows()));
        }

        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion)
    {
        // On destroy, notify source channel that all flows going through this
        // channel have been broken
        if (!level.isClientSide())
        {
            level.getBlockEntity(pos, TFCBlockEntities.CHANNEL.get()).ifPresent(
                channel -> channel.notifyBrokenLink(channel.getNumberOfFlows()));
        }

        super.onBlockExploded(state, level, pos, explosion);
    }

    public ChannelBlock(ExtendedProperties properties)
    {
        super(properties);
        this.registerDefaultState(
            this.defaultBlockState()
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(NORTH, false)
                .setValue(DOWN, false)
                .setValue(TRIGGERED, false)
                .setValue(WITH_METAL, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES[(state.getValue(NORTH) ? 1 << Direction.NORTH.get2DDataValue() : 0) |
            (state.getValue(EAST) ? 1 << Direction.EAST.get2DDataValue() : 0) |
            (state.getValue(SOUTH) ? 1 << Direction.SOUTH.get2DDataValue() : 0) |
            (state.getValue(WEST) ? 1 << Direction.WEST.get2DDataValue() : 0)];
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState adjacentState, LevelAccessor level, BlockPos pos, BlockPos adjacentPos)
    {
        for (Direction dir : Helpers.DIRECTIONS)
        {
            if (dir == Direction.UP)
                continue;

            // When going down, allow >1 block distance
            byte maxDistance = dir == Direction.DOWN ? Byte.MAX_VALUE : 1;

            boolean isAdjacentConnectable = false;

            for (byte i = 1; i < maxDistance + 1; i++)
            {
                BlockPos relative = pos.relative(dir, i);
                BlockState blockState = level.getBlockState(relative);
                Block block = blockState.getBlock();

                if (block instanceof ChannelBlock || block instanceof CrucibleBlock || block instanceof MoldTableBlock)
                {
                    isAdjacentConnectable = true;
                    break;
                }
                else if (!blockState.isAir())
                {
                    break;
                }
            }

            state = state.setValue(DirectionPropertyBlock.getProperty(dir), isAdjacentConnectable);
        }

        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston)
    {
        boolean isLocationPowered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean isChannelPowered = state.getValue(TRIGGERED);
        if (isLocationPowered && !isChannelPowered)
        {
            activate(level, pos);
            level.setBlockAndUpdate(pos, state.setValue(TRIGGERED, true));
        }
        else if (!isLocationPowered && isChannelPowered)
        {
            level.setBlockAndUpdate(pos, state.setValue(TRIGGERED, false));
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(EAST, SOUTH, WEST, NORTH, DOWN, TRIGGERED, WITH_METAL);
    }

    /***
     * Right-clicking a channel block will cause it to try to find crucibles
     * adjacent to it and start a flow if appropriate.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        if (player instanceof ServerPlayer)
        {
            activate(level, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public boolean activate(LevelAccessor level, BlockPos pos)
    {
        for (Direction dir : Direction.Plane.HORIZONTAL)
        {
            Optional<CrucibleBlockEntity> crucible = level.getBlockEntity(pos.relative(dir),
                TFCBlockEntities.CRUCIBLE.get());
            if (crucible.isPresent())
            {
                ChannelFlow.fromCrucible(level, crucible.get(), pos);
                return true;
            }
        }
        return false;
    }
}
