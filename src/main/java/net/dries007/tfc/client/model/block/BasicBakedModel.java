/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client.model.block;

import javax.annotation.Nonnull;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraftforge.client.model.data.IDynamicBakedModel;

public interface BasicBakedModel extends IDynamicBakedModel
{
    @Override
    default boolean useAmbientOcclusion()
    {
        return true;
    }

    @Override
    default boolean isGui3d()
    {
        return false;
    }

    @Override
    default boolean usesBlockLight()
    {
        return true;
    }

    @Override
    default boolean isCustomRenderer()
    {
        return false;
    }

    @Override
    @Nonnull
    default ItemOverrides getOverrides()
    {
        return ItemOverrides.EMPTY;
    }
}
