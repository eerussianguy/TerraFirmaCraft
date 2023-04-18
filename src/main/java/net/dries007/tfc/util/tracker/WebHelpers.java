package net.dries007.tfc.util.tracker;

import java.util.LinkedList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blockentities.RotatingBlockEntity;
import net.dries007.tfc.common.blocks.Rotator;
import net.dries007.tfc.util.Helpers;

public final class WebHelpers
{
    public static void rotatorAdded(Level level, BlockPos pos, RotatingBlockEntity blockEntity)
    {
        if (!level.isClientSide() && level.isLoaded(pos))
        {
            insertNewSource(level, pos, blockEntity);
        }
    }

    private static void insertNewSource(Level level, BlockPos pos, RotatingBlockEntity blockEntity)
    {
        for (RotatingBlockEntity neighbor : getConnections(blockEntity))
        {
            final int mySpeed = getRotationBetween(blockEntity, neighbor);
            final int otherSpeed = getRotationBetween(neighbor, blockEntity);
            if (otherSpeed == 0 && mySpeed == 0)
            {
                continue;
            }

            if (mySpeed != otherSpeed && (mySpeed != 0 && otherSpeed != 0))
            {
                // incompatible rotation directions
                level.destroyBlock(pos, true);
                return;
            }
            else
            {
                // power the other block entity
                if (Math.abs(mySpeed) >= Math.abs(otherSpeed))
                {
                    if (!blockEntity.hasWeb() || (blockEntity.getWeb() != null && neighbor.getWeb() != null && blockEntity.getWeb().equals(neighbor.getWeb())))
                    {
                        // check for cycle
                    }

                    neighbor.setSource(pos);
                    neighbor.setSpeed(mySpeed);

                    insertNewSource(level, neighbor.getBlockPos(), neighbor);
                    continue;
                }
            }
            if (otherSpeed != mySpeed)
            {
                neighbor.setSource(pos);
                neighbor.setSpeed(mySpeed);

                insertNewSource(level, neighbor.getBlockPos(), neighbor);
            }
        }
    }

    public static void removeConnection(Level level, BlockPos pos, @Nullable RotatingBlockEntity blockEntity)
    {
        if (!level.isClientSide && blockEntity != null)
        {
            for (BlockPos neighborPos : getValidConnectionLocations(blockEntity))
            {
                final BlockState state = level.getBlockState(neighborPos);
                if (state.getBlock() instanceof Rotator rotator && level.getBlockEntity(neighborPos) instanceof RotatingBlockEntity neighbor)
                {
                    if (neighbor.getSource() == null || !neighbor.getSource().equals(pos))
                    {
                        continue;
                    }
                    deleteSourceEverywhere(level, pos, blockEntity);
                }
            }
        }
    }

    private static void deleteSourceEverywhere(Level level, BlockPos pos, RotatingBlockEntity blockEntity)
    {
        final List<RotatingBlockEntity> candidateSources = new LinkedList<>();
        final List<BlockPos> checks = new LinkedList<>();
        checks.add(pos);
        final BlockPos missingPlace = blockEntity.hasSource() ? blockEntity.getSource() : null;

        while (!checks.isEmpty())
        {
            final BlockPos checkPos = checks.remove(0);
            if (level.getBlockEntity(checkPos) instanceof RotatingBlockEntity rotator)
            {
                rotator.removeSource();

                for (RotatingBlockEntity neighbor : getConnections(blockEntity))
                {
                    if (neighbor.getBlockPos().equals(missingPlace) || neighbor.getSource() == null || !neighbor.getSource().equals(checkPos)) continue;

                    if (neighbor.isSource())
                    {
                        candidateSources.add(neighbor);
                    }
                    checks.add(neighbor.getBlockPos());
                }
            }
        }

        for (RotatingBlockEntity neighbor : candidateSources)
        {
            if (neighbor.hasSource() || neighbor.isSource())
            {
                insertNewSource(level, pos, neighbor);
                return;
            }
        }
    }

    /**
     * @return 1 (clockwise) 0 (nothing) -1 (counterclockwise)
     */
    private static int getRotationBetween(RotatingBlockEntity from, RotatingBlockEntity to)
    {
        final BlockState fromState = from.getBlockState();
        final BlockState toState = to.getBlockState();
        final Level level = from.getLevel();

        if (fromState.getBlock() instanceof Rotator fromRotate && toState.getBlock() instanceof Rotator toRotate && level != null)
        {
            final BlockPos dPos = to.getBlockPos().subtract(from.getBlockPos());
            final Direction facing = Direction.getNearest(dPos.getX(), dPos.getY(), dPos.getZ());

            boolean alignedAxes = true;
            for (Direction.Axis axis : Direction.Axis.VALUES)
            {
                if (axis != facing.getAxis() && axis.choose(dPos.getX(), dPos.getY(), dPos.getZ()) != 0)
                {
                    alignedAxes = false;
                    break;
                }
            }

            final boolean axiallyConnected = alignedAxes && fromRotate.hasShaftTowards(level, from.getBlockPos(), fromState, facing) && toRotate.hasShaftTowards(level, to.getBlockPos(), toState, facing.getOpposite());

            // all types of custom rotation connections should basically be implemented here.

            if (axiallyConnected)
            {
                return getAxisModifier(from, facing) * getAxisModifier(to, facing.getOpposite());
            }
        }
        return 0;
    }

    private static int getAxisModifier(RotatingBlockEntity blockEntity, Direction facing)
    {
        // if we have behavior that changes the rotation direction of a thing, that goes here
        return 1;
    }

    @Nullable
    private static RotatingBlockEntity getPossibleNeighbor(RotatingBlockEntity current, BlockPos otherPos)
    {
        final Level level = current.getLevel();
        assert level != null;
        final BlockState otherState = level.getBlockState(otherPos);
        if (otherState.getBlock() instanceof Rotator && level.getBlockEntity(otherPos) instanceof RotatingBlockEntity neighbor)
        {
            return neighbor;
        }
        return null;
    }

    private static List<RotatingBlockEntity> getConnections(RotatingBlockEntity blockEntity)
    {
        final List<RotatingBlockEntity> list = new LinkedList<>();
        for (BlockPos pos : getValidConnectionLocations(blockEntity))
        {
            final RotatingBlockEntity neighbor = getPossibleNeighbor(blockEntity, pos);
            if (neighbor != null)
            {
                list.add(neighbor);
            }
        }
        return list;
    }

    private static List<BlockPos> getValidConnectionLocations(RotatingBlockEntity blockEntity)
    {
        final List<BlockPos> list = new LinkedList<>();

        // noinspection deprecation
        if (blockEntity.getLevel() == null || !blockEntity.getLevel().isAreaLoaded(blockEntity.getBlockPos(), 1))
        {
            return list;
        }
        for (Direction d : Helpers.DIRECTIONS)
        {
            list.add(blockEntity.getBlockPos().relative(d));
        }

        final BlockState state = blockEntity.getBlockState();
        if (state.getBlock() instanceof Rotator rotator)
        {
            return blockEntity.addConnectedPositions(rotator, state, list);
        }
        return list;

    }
}
