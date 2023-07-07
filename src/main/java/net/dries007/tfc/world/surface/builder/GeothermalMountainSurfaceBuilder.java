package net.dries007.tfc.world.surface.builder;

import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceStates;

public class GeothermalMountainSurfaceBuilder extends MountainSurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = GeothermalMountainSurfaceBuilder::new;

    public GeothermalMountainSurfaceBuilder(long seed)
    {
        super(seed);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        boolean needsBuild = false;
        final int seaLevel = context.getSeaLevel();
        for (int y = startY; y >= endY; --y)
        {
            if (y == seaLevel + 2)
            {
                context.setBlockState(y, SurfaceStates.RAW);
            }
            else if (y == seaLevel + 1)
            {
                context.setBlockState(y, SurfaceStates.ORANGE_MICROBIAL_MAT);
            }
            else if (y == seaLevel || y == seaLevel - 1)
            {
                context.setBlockState(y, SurfaceStates.YELLOW_MICROBIAL_MAT);
            }
            else if (y == seaLevel - 3 || y == seaLevel - 2)
            {
                context.setBlockState(y, SurfaceStates.BLUE_MICROBIAL_MAT);
            }
            else
            {
                needsBuild = true;
            }
        }
        super.buildSurface(context, startY, endY);
    }
}
