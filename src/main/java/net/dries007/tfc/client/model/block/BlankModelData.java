/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client.model.block;

import javax.annotation.Nullable;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * todo: 1.19. rewrite me lol (russian knows how)
 */
public interface BlankModelData extends IModelData
{
    @Override
    default boolean hasProperty(ModelProperty<?> prop)
    {
        return false;
    }

    @Nullable
    @Override
    default <T> T getData(ModelProperty<T> prop)
    {
        return null;
    }

    @Nullable
    @Override
    default <T> T setData(ModelProperty<T> prop, T data)
    {
        return null;
    }
}
