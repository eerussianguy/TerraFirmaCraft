/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import net.dries007.tfc.common.blocks.RiverWaterBlock;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.chunkdata.RockData;
import net.dries007.tfc.world.river.Flow;
import org.jetbrains.annotations.Nullable;

public class StreamGenerator extends AbstractStreamGenerator
{
    public StreamGenerator(long seed)
    {
        super(seed);
    }

    @Nullable
    @Override
    protected BlockPos findValidDrainPos(LevelAccessor level, ChunkPos chunkPos, Biome[] biomes)
    {
        BlockPos pos = new BlockPos(chunkPos.getMinBlockX() + random.nextInt(16), getSeaLevel(), chunkPos.getMinBlockZ() + random.nextInt(16));
        if (TFCBiomes.getExtensionOrThrow(level, biomes[(pos.getX() & 15) + 16 * (pos.getZ() & 15)]).variants().isLakeOrRiver())
        {
            return pos;
        }
        return null;
    }

    @Override
    protected int getSeaLevel()
    {
        return TFCChunkGenerator.SEA_LEVEL_Y;
    }

    @Override
    protected boolean isValidPiece(StreamPiece piece)
    {
        return true;
    }

    public void buildHeightDependentStreams(LevelAccessor level, ChunkAccess chunk, ChunkPos chunkPos, Biome[] biomes, @Nullable TFCChunkGenerator.IntArray256 data, ChunkData chunkData)
    {
        if (data == null)
        {
            //TerraFirmaCraft.LOGGER.info("Failed to get sample for {}", chunkPos);
            return;
        }
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        final int[] samples = data.values();
        final BlockState water = TFCBlocks.RIVER_WATER.get().defaultBlockState();
        final RockData rock = chunkData.getRockData();
        for (StreamStructure stream : generateStreamsAroundChunk(level, chunkPos, biomes, chunkData))
        {
            for (StreamPiece piece : stream.getPieces())
            {
                int xPos = (int) piece.getX();
                int zPos = (int) piece.getZ();
                for (int x = 0; x < piece.getWidth(); x++)
                {
                    for (int z = 0; z < piece.getWidth(); z++)
                    {
                        // if we are in the same section as the one we're placing in
                        final int actualX = xPos + x;
                        final int actualZ = zPos + z;
                        if (actualX >> 4 == chunkPos.x && actualZ >> 4 == chunkPos.z)
                        {
                            // Intersect the chunk at hand
                            Flow flow = piece.getFlow(x, z);
                            if (flow != Flow.NONE)
                            {
                                final int surfaceHeight = samples[(actualX & 15) + 16 * (actualZ & 15)];
                                mutablePos.set(actualX, surfaceHeight, actualZ);
                                final BlockState flowing = water.setValue(RiverWaterBlock.FLOW, flow);
                                chunk.setBlockState(mutablePos, flowing, false);

                                mutablePos.move(0, -1, 0);
                                final BlockState gravel = rock.getRock(actualX, surfaceHeight - 1, actualZ).gravel().defaultBlockState();
                                chunk.setBlockState(mutablePos, gravel, false);
                            }
                        }
                    }
                }
            }
        }
    }
}
