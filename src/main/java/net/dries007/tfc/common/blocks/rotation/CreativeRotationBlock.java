/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.rotation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.rotation.RotatingBlockEntity;
import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.wood.ExtendedRotatedPillarBlock;
import net.dries007.tfc.common.blocks.wood.Wood;

public class CreativeRotationBlock extends ExtendedRotatedPillarBlock implements EntityBlockExtension, ConnectedAxleBlock
{
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    public CreativeRotationBlock(ExtendedProperties properties)
    {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(AXIS);
    }

    @Override
    public AxleBlock getAxle()
    {
        return (AxleBlock) TFCBlocks.WOODS.get(Wood.ACACIA).get(Wood.BlockType.AXLE).get();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult result)
    {
        Direction.Axis axis = state.getValue(AXIS);
        Direction direction = result.getDirection();
        return level.getBlockEntity(pos, TFCBlockEntities.CREATIVE_MOTOR.get()).map(motor -> {
            Direction.Axis interactableAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
            if (interactableAxis == direction.getAxis())
            {
                if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE)
                {
                    motor.incrementSpeed();
                }
                else
                {
                    motor.decrementSpeed();
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }).orElse(InteractionResult.PASS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (level.getBlockEntity(pos) instanceof RotatingBlockEntity entity)
        {
            entity.destroyIfInvalid(level, pos);
        }
    }
}
