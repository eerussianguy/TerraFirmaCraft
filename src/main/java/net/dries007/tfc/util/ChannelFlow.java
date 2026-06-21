/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blockentities.ChannelBlockEntity;
import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blockentities.MoldTableBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;

public class ChannelFlow
{
    public static void fromCrucible(LevelAccessor level, CrucibleBlockEntity source, BlockPos originChannel)
    {
        // This checks that metal is present and molten
        Optional<IFluidHandler> iFldHandler = MoldTableBlockEntity.getFluidHandlerIfAppropriate(source, Optional.empty());
        if (iFldHandler.isEmpty())
        {
            return;
        }

        // Get fluid properties
        FluidStack fluidStack = iFldHandler.get().drain(1, IFluidHandler.FluidAction.SIMULATE);
        Fluid fluid = fluidStack.getFluid();
        ResourceLocation fluidKey = BuiltInRegistries.FLUID.getKey(fluid);

        // Find paths
        int counter = 0;
        final BiMap<Integer, BlockPos> channelsAndMolds = HashBiMap.create();

        // Only channels are keys to this map, but the list of neighbors
        // contains both channel and mold
        final Map<BlockPos, List<BlockPos>> neighbors = new HashMap<>();

        final Deque<BlockPos> pendingVisit = new ArrayDeque<>();
        final Set<BlockPos> molds = new HashSet<>();

        pendingVisit.add(originChannel);
        while (pendingVisit.size() > 0)
        {
            BlockPos current = pendingVisit.pop();
            if (channelsAndMolds.containsValue(current))
                continue;

            channelsAndMolds.put(counter, current);
            counter++;

            // Find adjacent channels and molds
            List<BlockPos> adjacentChannels = findAdjacent(level, current, false);
            List<BlockPos> adjacentMolds = findAdjacent(level, current, true);

            // Channels should be visited in the future so that
            // the entire net of channels is found
            adjacentChannels.stream().forEach(adj -> pendingVisit.add(adj));

            // Save molds for later
            molds.addAll(adjacentMolds);

            // The neighbors of the channel are both the channel and mold
            neighbors.put(current, adjacentChannels);
            neighbors.get(current).addAll(adjacentMolds);
        }

        // Remove molds if they meet the following criteria:
        // - The output stack is not empty
        // - The mold could not accept the fluid
        // Removing them now removes molds that will get disconected
        // after a single tick and without getting any fluid, which
        // improves performance a bit and most importantly prevents
        // a flickering rendered flow.
        molds.removeIf(
            pos -> {
                Optional<MoldTableBlockEntity> moldEnt = level.getBlockEntity(pos, TFCBlockEntities.MOLD_TABLE.get());

                if (moldEnt.isEmpty())
                    return true;
                if (!moldEnt.get().getOutputStack().isEmpty())
                    return true;
                final FluidStack outputDrop = iFldHandler.get().drain(1, IFluidHandler.FluidAction.SIMULATE);
                return !couldBeFilled(moldEnt.get().getInventory(), outputDrop, MoldTableBlockEntity.MOLD_SLOT);
            });

        // Early exit if no (valid) molds are connected
        if (molds.size() == 0)
        {
            return;
        }

        // Add a numeric index to the molds
        for (BlockPos mold : molds)
        {
            channelsAndMolds.put(counter, mold);
            counter++;
        }

        int[][] graph = new int[channelsAndMolds.size()][channelsAndMolds.size()];
        double[][] heuristic = new double[channelsAndMolds.size()][channelsAndMolds.size()];

        // channel are the x of the graph adjacency matrix, while neighborChannelOrMold
        // are the y. This means that all channels are two-way connected to each other
        // (flow can go in both directions), while channels are connected to molds in
        // one-way (flow can only go INTO the mold)
        // This means that the flow from source to mold will
        // never cross another mold in the net.
        for (BlockPos channel : channelsAndMolds.values())
        {
            if (!neighbors.containsKey(channel))
                continue; // Molds dont have neighbors

            for (BlockPos neighborChannelOrMold : neighbors.get(channel))
            {
                int x = channelsAndMolds.inverse().get(channel);

                // Might not be there if the mold table was removed
                // because it was not ready to accept flow
                if (!channelsAndMolds.containsValue(neighborChannelOrMold))
                    continue;

                int y = channelsAndMolds.inverse().get(neighborChannelOrMold);
                graph[x][y] = 1;
                if (heuristic[x][y] == 0)
                {
                    heuristic[x][y] = heuristic[y][x] = Math.sqrt(channel.distSqr(neighborChannelOrMold));
                }
            }
        }

        // *** For each blockpos, saves the direction where flow is coming from*/
        final Map<BlockPos, Pair<Direction, Byte>> flowSource = new HashMap<>();

        // *** Saves the number of paths that go through each channel */
        final Map<BlockPos, Integer> numFlows = new HashMap<>();

        for (BlockPos mold : molds)
        {
            List<BlockPos> path = aStar(
                graph,
                heuristic,
                0,
                channelsAndMolds.inverse().get(mold)).stream()
                .map(channelsAndMolds::get).toList(); // index to BlockPos
            // goal (mold) to start (first channel)

            for (int i = 0; i < path.size() - 1; i++)
            {
                BlockPos currentChannel = path.get(i);
                BlockPos channelSource = path.get(i + 1);
                BlockPos relative = channelSource.offset(currentChannel.multiply(-1));
                int distance = Math.abs(relative.getX() + relative.getY() + relative.getZ());
                BlockPos normal = new BlockPos(relative.getX() / distance, relative.getY() / distance,
                    relative.getZ() / distance);
                flowSource.put(currentChannel,
                    Pair.of(Direction.fromDelta(normal.getX(), normal.getY(), normal.getZ()), (byte) distance));
                numFlows.put(channelSource, numFlows.getOrDefault(channelSource, 0) + 1);
            }
        }

        for (BlockPos channelOrMold : flowSource.keySet())
        {
            level.getBlockEntity(channelOrMold, TFCBlockEntities.CHANNEL.get()).ifPresent(
                channel -> {
                    channel.setLinkProperties(
                        flowSource.get(channelOrMold),
                        true,
                        numFlows.get(channelOrMold),
                        fluidKey);
                });
            level.getBlockEntity(channelOrMold, TFCBlockEntities.MOLD_TABLE.get()).ifPresent(
                mold -> mold.setSource(
                    source.getBlockPos(), fluid, flowSource.get(channelOrMold)));
        }
        BlockPos pos = source.getBlockPos().offset(originChannel.multiply(-1));
        level.getBlockEntity(originChannel, TFCBlockEntities.CHANNEL.get()).get().setLinkProperties(
            Pair.of(Direction.fromDelta(pos.getX(), pos.getY(), pos.getZ()), (byte) 1),
            false,
            numFlows.get(originChannel),
            fluidKey);
    }

    private static List<BlockPos> findAdjacent(LevelAccessor level, BlockPos current, boolean findMolds)
    {
        final List<BlockPos> adjacent = new ArrayList<>();
        for (Direction dir : Direction.values())
        {
            if (dir == Direction.UP)
                continue;

            // When going down, allow >1 block distance
            byte maxDistance = dir == Direction.DOWN ? Byte.MAX_VALUE : 1;

            for (byte i = 1; i < maxDistance + 1; i++)
            {
                BlockPos relative = current.relative(dir, i);
                BlockState blockState = level.getBlockState(relative);
                BlockEntity blockEntity = level.getBlockEntity(relative);

                if (findMolds && blockEntity instanceof MoldTableBlockEntity mold && !mold.hasSource())
                {
                    adjacent.add(relative);
                    break;
                }
                else if (!findMolds && blockEntity instanceof ChannelBlockEntity channel && !channel.hasFlow())
                {
                    adjacent.add(relative);
                    break;
                }
                else if (!blockState.isAir())
                {
                    break;
                }
            }
        }
        return adjacent;
    }

    /**
     * Finds the shortest distance between two nodes using the A-star algorithm
     *
     * @param graph     an adjacency-matrix-representation of the graph where (x,y)
     *                  is the weight of the edge or 0 if there is no edge.
     * @param heuristic an estimation of distance from node x to y that is
     *                  guaranteed to be lower than the actual distance. E.g.
     *                  straight-line distance
     * @param start     the node to start from.
     * @param goal      the node we're searching for
     * @return The path from goal to start, both included
     *
     * Adapted from
     * https://github.com/ClaasM/Algorithms/blob/master/src/a_star/java/simple/AStar.java
     */
    private static List<Integer> aStar(int[][] graph, double[][] heuristic, int start, int goal)
    {
        // This contains the distances from the start node to all other nodes
        int[] distances = new int[graph.length];
        // Initializing with a distance of "Infinity"
        Arrays.fill(distances, Integer.MAX_VALUE);
        // The distance from the start node to itself is of course 0
        distances[start] = 0;

        int[] parent = new int[graph.length];

        // This contains the priorities with which to visit the nodes, calculated using
        // the heuristic.
        double[] priorities = new double[graph.length];
        // Initializing with a priority of "Infinity"
        Arrays.fill(priorities, Integer.MAX_VALUE);
        // start node has a priority equal to straight line distance to goal. It will be
        // the first to be expanded.
        priorities[start] = heuristic[start][goal];

        // This contains whether a node was already visited
        boolean[] visited = new boolean[graph.length];

        // While there are nodes left to visit...
        while (true)
        {

            // ... find the node with the currently lowest priority...
            double lowestPriority = Integer.MAX_VALUE;
            int lowestPriorityIndex = -1;
            for (int i = 0; i < priorities.length; i++)
            {
                // ... by going through all nodes that haven't been visited yet
                if (priorities[i] < lowestPriority && !visited[i])
                {
                    lowestPriority = priorities[i];
                    lowestPriorityIndex = i;
                }
            }

            if (lowestPriorityIndex == -1)
            {
                // There was no node not yet visited --> Node not found
                throw new IllegalArgumentException("Illegal graph! No connection between start and end.");
            }
            else if (lowestPriorityIndex == goal)
            {
                // Goal node found
                ArrayList<Integer> finalPath = new ArrayList<>();
                int currentIndex = lowestPriorityIndex;
                while (currentIndex != start)
                {
                    finalPath.add(currentIndex);
                    currentIndex = parent[currentIndex];
                }
                finalPath.add(start);
                return finalPath;
            }

            // ...then, for all neighboring nodes that haven't been visited yet....
            for (int i = 0; i < graph[lowestPriorityIndex].length; i++)
            {
                if (graph[lowestPriorityIndex][i] != 0 && !visited[i])
                {
                    // ...if the path over this edge is shorter...
                    if (distances[lowestPriorityIndex] + graph[lowestPriorityIndex][i] < distances[i])
                    {
                        // ...save this path as new shortest path
                        distances[i] = distances[lowestPriorityIndex] + graph[lowestPriorityIndex][i];
                        parent[i] = lowestPriorityIndex;
                        // ...and set the priority with which we should continue with this node
                        priorities[i] = distances[i] + heuristic[i][goal];
                    }
                }
            }

            // Lastly, note that we are finished with this node.
            visited[lowestPriorityIndex] = true;
        }
    }

    public static boolean couldBeFilled(IItemHandlerModifiable inventory, FluidStack fluidStack, int slot)
    {
        if (!fluidStack.isEmpty())
        {
            final ItemStack stack = inventory.getStackInSlot(slot);
            final @Nullable IFluidHandler fluidCap = stack.getCapability(Capabilities.FluidHandler.ITEM);
            if (fluidCap != null)
            {
                int filled = fluidCap.fill(fluidStack, IFluidHandler.FluidAction.SIMULATE);
                return filled > 0;
            }
        }
        return false;
    }

    public static void damageEntities(LevelAccessor level, BlockPos pos, int n)
    {
        if (level.isClientSide()) return;
        AABB moltenFall = new AABB(
            pos.getX() + 6.0 / 16, pos.getY() + 4.0 / 16, pos.getZ() + 6.0 / 16,
            pos.getX() + +10.0 / 16, pos.getY() + (n - 1), pos.getZ() + +10.0 / 16
        );
        level.getEntitiesOfClass(LivingEntity.class, moltenFall).forEach(Entity::lavaHurt);
    }
}
