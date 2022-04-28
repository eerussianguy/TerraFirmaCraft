/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world;

import java.util.Map;

import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.dries007.tfc.world.biome.BiomeVariants;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.noise.ChunkNoiseSamplingSettings;

public class ChunkNoiseFillerLite
{
    private final int[] sample; // the actual sampled heights
    private final int[] lowResSample; // a less accurate sample

    private final LevelAccessor level;
    private final Map<BiomeVariants, BiomeNoiseSampler> biomeNoiseSamplers; // Biome -> Noise Samplers
    private final Object2DoubleMap<BiomeNoiseSampler> columnBiomeNoiseSamplers; // Per column weighted map of biome noises samplers
    private final Object2DoubleMap<Biome> biomeWeights1; // Local biome weights, for individual column adjustment
    private final Object2DoubleMap<Biome>[] sampledBiomeWeights; // 7x7 array of biome weights, at quart pos resolution
    private final ChunkNoiseSamplingSettings settings;

    private final int chunkMinX, chunkMinZ; // Min block positions for the chunk
    private int blockX, blockZ; // Absolute x/z positions
    private int localX, localZ; // Chunk-local x/z

    public ChunkNoiseFillerLite(LevelAccessor level, ChunkPos chunkPos, Object2DoubleMap<Biome>[] sampledBiomeWeights, Map<BiomeVariants, BiomeNoiseSampler> biomeNoiseSamplers, ChunkNoiseSamplingSettings settings)
    {
        this.level = level;
        this.biomeNoiseSamplers = biomeNoiseSamplers;
        this.columnBiomeNoiseSamplers = new Object2DoubleOpenHashMap<>();
        this.biomeWeights1 = new Object2DoubleOpenHashMap<>();
        this.sampledBiomeWeights = sampledBiomeWeights;
        this.settings = settings;
        this.chunkMinX = chunkPos.getMinBlockX();
        this.chunkMinZ = chunkPos.getMinBlockZ();
        this.sample = new int[16 * 16];
        this.lowResSample = new int[4 * 4];
    }

    public int[] getLowResSample()
    {
        return lowResSample;
    }

    public int[] getSample()
    {
        return sample;
    }

    public void fillFromNoiseLowRes()
    {
        for (int cellX = 0; cellX < settings.cellCountXZ(); cellX += 4)
        {
            for (int cellZ = 0; cellZ < settings.cellCountXZ(); cellZ += 4)
            {
                // skip cell Y
                for (int localCellX = 0; localCellX < settings.cellWidth(); localCellX++)
                {
                    blockX = chunkMinX + cellX * settings.cellWidth() + localCellX;
                    localX = blockX & 15;

                    // cannot update for x here because we first need to update for yz. So we do all three each time per cell
                    for (int localCellZ = 0; localCellZ < settings.cellWidth(); localCellZ++)
                    {
                        blockZ = chunkMinZ + cellZ * settings.cellWidth() + localCellZ;
                        localZ = blockZ & 15;

                        lowResSample[(cellX / 4) + cellZ] = sampleColumnHeightAndBiome();
                    }
                }
            }
        }
    }

    public void fillFromNoise()
    {
        for (int cellX = 0; cellX < settings.cellCountXZ(); cellX++)
        {
            for (int cellZ = 0; cellZ < settings.cellCountXZ(); cellZ++)
            {
                // skip cell Y
                for (int localCellX = 0; localCellX < settings.cellWidth(); localCellX++)
                {
                    blockX = chunkMinX + cellX * settings.cellWidth() + localCellX;
                    localX = blockX & 15;

                    // cannot update for x here because we first need to update for yz. So we do all three each time per cell
                    for (int localCellZ = 0; localCellZ < settings.cellWidth(); localCellZ++)
                    {
                        blockZ = chunkMinZ + cellZ * settings.cellWidth() + localCellZ;
                        localZ = blockZ & 15;

                        sample[localX + 16 * localZ] = sampleColumnHeightAndBiome();
                    }
                }
            }
        }
    }

    /**
     * For a given (x, z) position, samples the provided biome weight map to calculate the height at that location, and the biome
     * @return The maximum height at this location
     */
    private int sampleColumnHeightAndBiome()
    {
        prepareColumnBiomeWeights();
        columnBiomeNoiseSamplers.clear();

        // Requires the column to be initialized (just x/z)
        double totalHeight = 0, riverHeight = 0, shoreHeight = 0;
        double riverWeight = 0, shoreWeight = 0;
        Biome biomeAt = null, normalBiomeAt = null, riverBiomeAt = null, shoreBiomeAt = null;
        double maxNormalWeight = 0, maxRiverWeight = 0, maxShoreWeight = 0; // Partition on biome type

        Biome oceanicBiomeAt = null;
        double oceanicWeight = 0, maxOceanicWeight = 0; // Partition on ocean/non-ocean or water type.

        for (Object2DoubleMap.Entry<Biome> entry : biomeWeights1.object2DoubleEntrySet())
        {
            final double weight = entry.getDoubleValue();
            final BiomeVariants variants = TFCBiomes.getExtensionOrThrow(level, entry.getKey()).variants();
            final BiomeNoiseSampler sampler = biomeNoiseSamplers.get(variants);

            if (columnBiomeNoiseSamplers.containsKey(sampler))
            {
                columnBiomeNoiseSamplers.mergeDouble(sampler, weight, Double::sum);
            }
            else
            {
                sampler.setColumn(blockX, blockZ);
                columnBiomeNoiseSamplers.put(sampler, weight);
            }

            double height = weight * sampler.height();
            totalHeight += height;

            /*// Partition into river / shore / normal for standard biome transformations
            if (variants.isRiver())
            {
                riverHeight += height;
                riverWeight += weight;
                if (maxRiverWeight < weight)
                {
                    riverBiomeAt = entry.getKey();
                    maxRiverWeight = weight;
                }
            }
            else if (variants.isShore())
            {
                shoreHeight += height;
                shoreWeight += weight;
                if (maxShoreWeight < weight)
                {
                    shoreBiomeAt = entry.getKey();
                    maxShoreWeight = weight;
                }
            }*/
            if (variants.isLakeOrRiver() || variants.isShore())
            {
                return 63; // todo bad ? necessary?
            }
            else if (maxNormalWeight < weight)
            {
                normalBiomeAt = entry.getKey();
                maxNormalWeight = weight;
            }

            // Also record oceanic biome types
            if (variants.isSalty())
            {
                oceanicWeight += weight;
                if (maxOceanicWeight < weight)
                {
                    oceanicBiomeAt = entry.getKey();
                    maxOceanicWeight = weight;
                }
            }
        }

        double actualHeight = totalHeight;
        if (riverWeight > 0.6 && riverBiomeAt != null)
        {
            // Primarily river biomes.
            // Based on the oceanic weight, we may apply a modifier which scales rivers down, and creates sharp cliffs near river borders.
            // If oceanic weight is high, this effect is ignored, and we intentionally weight towards the oceanic biome.
            double aboveWaterDelta = Mth.clamp(actualHeight - riverHeight / riverWeight, 0, 20);
            double adjustedAboveWaterDelta = 0.02 * aboveWaterDelta * (40 - aboveWaterDelta) - 0.48;
            double actualHeightWithRiverContribution = riverHeight / riverWeight + adjustedAboveWaterDelta;

            // Contribution of ocean type biomes to the 'normal' weight.
            double normalWeight = 1 - riverWeight - shoreWeight;
            double oceanicContribution = Mth.clamp(oceanicWeight == 0 || normalWeight == 0 ? 0 : oceanicWeight / normalWeight, 0, 1);
            if (oceanicContribution < 0.5)
            {
                actualHeight = Mth.lerp(2 * oceanicContribution, actualHeightWithRiverContribution, actualHeight);
                biomeAt = riverBiomeAt;
            }
            else
            {
                // Consider this primarily an oceanic weight area, in biome only. Do not adjust the nominal height.
                biomeAt = oceanicBiomeAt;
            }
        }
        else if (riverWeight > 0 && normalBiomeAt != null)
        {
            double adjustedRiverWeight = 0.6 * riverWeight;
            actualHeight = (totalHeight - riverHeight) * ((1 - adjustedRiverWeight) / (1 - riverWeight)) + riverHeight * (adjustedRiverWeight / riverWeight);

            biomeAt = normalBiomeAt;
        }
        else if (normalBiomeAt != null)
        {
            biomeAt = normalBiomeAt;
        }

        if ((shoreWeight > 0.6 || maxShoreWeight > maxNormalWeight) && shoreBiomeAt != null)
        {
            // Flatten beaches above a threshold, creates cliffs where the beach ends
            double aboveWaterDelta = actualHeight - shoreHeight / shoreWeight;
            if (aboveWaterDelta > 0)
            {
                if (aboveWaterDelta > 20)
                {
                    aboveWaterDelta = 20;
                }
                double adjustedAboveWaterDelta = 0.02 * aboveWaterDelta * (40 - aboveWaterDelta) - 0.48;
                actualHeight = shoreHeight / shoreWeight + adjustedAboveWaterDelta;
            }
            biomeAt = shoreBiomeAt;
        }

        assert biomeAt != null;
        return (int) actualHeight;
    }

    private void prepareColumnBiomeWeights()
    {
        final int index4X = (localX >> 2) + 1;
        final int index4Z = (localZ >> 2) + 1;

        final double lerpX = (localX - ((localX >> 2) << 2)) * (1 / 4d);
        final double lerpZ = (localZ - ((localZ >> 2) << 2)) * (1 / 4d);

        biomeWeights1.clear();
        TFCChunkGenerator.sampleBiomesCornerContribution(biomeWeights1, sampledBiomeWeights[index4X + index4Z * 7], (1 - lerpX) * (1 - lerpZ));
        TFCChunkGenerator.sampleBiomesCornerContribution(biomeWeights1, sampledBiomeWeights[(index4X + 1) + index4Z * 7], lerpX * (1 - lerpZ));
        TFCChunkGenerator.sampleBiomesCornerContribution(biomeWeights1, sampledBiomeWeights[index4X + (index4Z + 1) * 7], (1 - lerpX) * lerpZ);
        TFCChunkGenerator.sampleBiomesCornerContribution(biomeWeights1, sampledBiomeWeights[(index4X + 1) + (index4Z + 1) * 7], lerpX * lerpZ);
    }

}
