/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.rotation;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blockentities.rotation.RotatingBlockEntity;
import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.IForgeBlockExtension;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.FluidProperty;
import net.dries007.tfc.common.fluids.IFluidLoggable;

public class AxleBlock extends RotatedPillarBlock implements IForgeBlockExtension, IFluidLoggable, EntityBlockExtension, ConnectedAxleBlock
{
    public static final FluidProperty FLUID = TFCBlockStateProperties.WATER;

    public static final VoxelShape SHAPE_Z = box(6, 6, 0, 10, 10, 16);
    public static final VoxelShape SHAPE_X = box(0, 6, 6, 16, 10, 10);
    public static final VoxelShape SHAPE_Y = box(6, 0, 6, 10, 16, 10);

    private final ExtendedProperties properties;
    private final Supplier<? extends WindmillBlock> windmill;
    private final ResourceLocation textureLocation;

    public AxleBlock(ExtendedProperties properties, Supplier<? extends WindmillBlock> windmill, ResourceLocation textureLocation)
    {
        super(properties.properties());

        this.properties = properties;
        this.windmill = windmill;
        this.textureLocation = textureLocation;

        registerDefaultState(getStateDefinition().any().setValue(AXIS, Direction.Axis.X).setValue(FLUID, FLUID.keyFor(Fluids.EMPTY)));
    }

    @Override
    public AxleBlock getAxle()
    {
        return this;
    }

    @Override
    public ResourceLocation getAxleTextureLocation()
    {
        return textureLocation;
    }

    public WindmillBlock getWindmill()
    {
        return windmill.get();
    }

    @Override
    public ExtendedProperties getExtendedProperties()
    {
        return properties;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (level.getBlockEntity(pos) instanceof RotatingBlockEntity entity)
        {
            entity.destroyIfInvalid(level, pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return switch(state.getValue(AXIS))
            {
                case X -> SHAPE_X;
                case Y -> SHAPE_Y;
                case Z -> SHAPE_Z;
            };
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        final BlockState state = super.getStateForPlacement(context);
        final FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        if (state != null && !fluidState.isEmpty())
        {
            return state.setValue(getFluidProperty(), getFluidProperty().keyForOrEmpty(fluidState.getType()));
        }
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos)
    {
        FluidHelpers.tickFluid(level, currentPos, state);
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(getFluidProperty()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state)
    {
        return IFluidLoggable.super.getFluidState(state);
    }

    @Override
    public FluidProperty getFluidProperty()
    {
        return FLUID;
    }
}
