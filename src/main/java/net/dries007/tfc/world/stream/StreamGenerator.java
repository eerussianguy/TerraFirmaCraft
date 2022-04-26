package net.dries007.tfc.world.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import net.dries007.tfc.world.TFCChunkGenerator;
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
}
