/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.feature.plant;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluid;

import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.plant.RotatableWaterPlantBlock;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.world.feature.BlockConfig;

public class RotatableWaterPlantFeature extends Feature<BlockConfig<RotatableWaterPlantBlock>>
{
    public static final Codec<BlockConfig<RotatableWaterPlantBlock>> CODEC = BlockConfig.codec(b -> b instanceof RotatableWaterPlantBlock t ? t : null, "Must be a " + RotatableWaterPlantBlock.class.getSimpleName());

    public RotatableWaterPlantFeature(Codec<BlockConfig<RotatableWaterPlantBlock>> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockConfig<RotatableWaterPlantBlock>> context)
    {
        final RandomSource random = context.random();
        final WorldGenLevel level = context.level();
        final BlockPos pos = context.origin().offset(0, random.nextInt(12), 0);
        if (!EnvironmentHelpers.isWorldgenReplaceable(level, pos))
            return false;
        BlockState state = context.config().block().defaultBlockState();
        for (Direction direction : Direction.allShuffled(random))
        {
            state = state.setValue(RotatableWaterPlantBlock.FACING, direction);
            if (state.canSurvive(level, pos))
            {
                final Fluid fluidAt = level.getFluidState(pos).getType();
                final boolean isOpen = fluidAt.isSame(TFCFluids.SALT_WATER.getSource());
                final BlockState waterloggedState = FluidHelpers.fillWithFluid(state, fluidAt);
                if (waterloggedState != null)
                {
                    setBlock(level, pos, waterloggedState.setValue(TFCBlockStateProperties.OPEN, isOpen));
                    return true;
                }
            }
        }
        return false;
    }
}
