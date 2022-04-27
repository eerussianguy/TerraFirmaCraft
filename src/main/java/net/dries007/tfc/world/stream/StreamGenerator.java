package net.dries007.tfc.world.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import net.dries007.tfc.common.blocks.RiverWaterBlock;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.world.TFCChunkGenerator;
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
    protected BlockPos findValidDrainPos(ChunkPos pos)
    {
        // todo: respect rivers
        return new BlockPos(pos.getMinBlockX() + random.nextInt(16), getSeaLevel(), pos.getMinBlockZ() + random.nextInt(16));
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

    public void buildHeightDependentRivers(ChunkAccess chunk, ChunkPos chunkPos, @Nullable TFCChunkGenerator.IntArray256 data)
    {
        if (data == null)
        {
            //TerraFirmaCraft.LOGGER.info("Failed to get sample for {}", chunkPos);
            return;
        }
        int[] samples = data.values();
        BlockState water = TFCBlocks.RIVER_WATER.get().defaultBlockState();
        for (StreamStructure stream : generateStreamsAroundChunk(chunkPos))
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
                        if ((xPos + x) >> 4 == chunkPos.x && (zPos + z) >> 4 == chunkPos.z)
                        {
                            // Intersect the chunk at hand
                            Flow flow = piece.getFlow(x, z);
                            if (flow != Flow.NONE)
                            {
                                // todo: this is not quite the right assumption
                                final int y = samples[x + 16 * z];
                                chunk.setBlockState(new BlockPos(xPos + x, y, zPos + z), water.setValue(RiverWaterBlock.FLOW, flow), false);
                            }
                        }
                    }
                }
            }
        }
    }
}
