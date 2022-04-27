package net.dries007.tfc.mixin;

import net.minecraft.world.level.chunk.ChunkAccess;

import net.dries007.tfc.world.IExtendedChunk;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkAccess.class)
public class ChunkAccessMixin implements IExtendedChunk
{
    @Unique
    private double[] weights = new double[0];

    @Override
    public double[] getWeights()
    {
        return weights;
    }

    @Override
    public void setWeights(double @NotNull[] newWeights)
    {
        weights = newWeights;
    }

}
