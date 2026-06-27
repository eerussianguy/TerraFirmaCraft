/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.dries007.tfc.common.blocks.GlassworkingStationBlock;
import net.dries007.tfc.common.blocks.rotation.GlassworkingWheelBlock;
import net.dries007.tfc.common.component.glass.GlassOperation;
import net.dries007.tfc.common.component.glass.GlassOperations;
import net.dries007.tfc.common.component.glass.GlassWorking;
import net.dries007.tfc.common.component.glass.IGlassworkingTool;
import net.dries007.tfc.util.Helpers;

/**
 * Holds a single glassworking tool, applying its operation to a blowpipe as it is rotated past by an adjacent
 * {@link net.dries007.tfc.common.blocks.rotation.GlassworkingWheelBlock}. Unlike powders applied by the hatch, tool
 * operations require the glass to be hot enough, so a too-cold blowpipe is skipped without recording a step.
 */
public class GlassworkingStationBlockEntity extends InventoryBlockEntity<ItemStackHandler>
{
    public GlassworkingStationBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCBlockEntities.GLASSWORKING_STATION.get(), pos, state, defaultInventory(1));
    }

    /**
     * Applies the held tool's operation to the given {@code blowpipe}, if a tool is present and the glass is hot enough. The
     * wheel is responsible for only calling this once per pass, so no cooldown is needed here.
     *
     * @return {@code true} if the operation was applied.
     */
    public boolean interactWithBlowpipe(ItemStack blowpipe)
    {
        if (level == null)
            return false;
        // The station must face the wheel that is working the blowpipe (e.g. a station facing north is activated by the wheel
        // to its north, which works the blowpipe on its south side).
        final Direction facing = getBlockState().getValue(GlassworkingStationBlock.FACING);
        if (!(level.getBlockState(getBlockPos().relative(facing)).getBlock() instanceof GlassworkingWheelBlock))
            return false;
        if (!(inventory.getStackInSlot(0).getItem() instanceof IGlassworkingTool tool))
            return false;
        final GlassOperations data = GlassWorking.get(blowpipe);
        if (data.isEmpty())
            return false;

        final GlassOperation op = tool.getOperation();
        if (op.clearsSteps())
        {
            // A marver resets the work regardless of temperature, but does nothing if there is nothing to clear.
            if (data.steps().isEmpty())
                return false;
        }
        else
        {
            if (!op.hasRequiredTemperature(blowpipe))
                return false;
            // A station cannot apply the same operation twice in a row.
            final var steps = data.steps();
            if (!steps.isEmpty() && steps.get(steps.size() - 1) == op)
                return false;
        }

        GlassWorking.apply(blowpipe, op);
        if (level instanceof ServerLevel server)
        {
            final BlockPos pos = getBlockPos();
            Helpers.playSound(level, pos, op.getSound());
            // A small burst at the worked glass to show the operation occurred. TODO tune particle type/position per operation.
            server.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.0);
        }
        return true;
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return stack.getItem() instanceof IGlassworkingTool;
    }
}
