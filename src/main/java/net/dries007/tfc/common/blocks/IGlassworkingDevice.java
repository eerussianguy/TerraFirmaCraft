/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Implemented by blocks placed adjacent to a {@link net.dries007.tfc.common.blocks.rotation.GlassworkingWheelBlock}, which
 * apply a glassworking operation to a blowpipe as it is rotated past them. Blocks implementing this should also be added to
 * the {@link net.dries007.tfc.common.TFCTags.Blocks#GLASSWORKING_WHEEL_INVISIBLE} tag so that the wheel does not eject the
 * blowpipe upon contact.
 */
public interface IGlassworkingDevice
{
    /**
     * Called by the glassworking wheel when a {@code blowpipe} is rotated adjacent to this device.
     *
     * @param blowpipe The blowpipe stack held in the wheel, which may be modified in place.
     * @return {@code true} if the device interacted with the blowpipe and the wheel should sync.
     */
    boolean interactWithBlowpipe(Level level, BlockPos pos, ItemStack blowpipe);
}
