/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.classic;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * This class is only to be used for rendering
 * It stores cached versions of the climate data on CLIENT ONLY
 */
public final class ClimateRenderHelper
{
    private static final Map<ChunkPos, ClimateData> MAP = new HashMap<>();
    private static final ClimateData DEFAULT = new ClimateData(15, 250);

    @Nonnull
    public static ClimateData get(BlockPos pos)
    {
        return get(new ChunkPos(pos));
    }

    @Nonnull
    public static ClimateData get(ChunkPos pos)
    {
        return MAP.getOrDefault(pos, DEFAULT);
    }

    public static void update(ChunkPos pos, float temperature, float rainfall)
    {
        MAP.put(pos, new ClimateData(temperature, rainfall));
    }

    public static class ClimateData
    {
        private final float baseTemp;
        private final float rainfall;

        ClimateData(float baseTemp, float rainfall)
        {
            this.baseTemp = baseTemp;
            this.rainfall = rainfall;
        }

        public float getTemperature()
        {
            return ClimateTFC.getMonthAdjTemp(baseTemp);
        }

        public float getRainfall()
        {
            return rainfall;
        }
    }
}
