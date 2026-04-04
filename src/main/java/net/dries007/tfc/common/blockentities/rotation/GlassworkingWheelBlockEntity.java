package net.dries007.tfc.common.blockentities.rotation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.TickableInventoryBlockEntity;
import net.dries007.tfc.common.blocks.HatchBlock;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.IHeat;
import net.dries007.tfc.common.component.heat.IHeatConsumer;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.rotation.NetworkAction;
import net.dries007.tfc.util.rotation.Node;
import net.dries007.tfc.util.rotation.SinkNode;

public class GlassworkingWheelBlockEntity extends TickableInventoryBlockEntity<ItemStackHandler> implements RotationSinkBlockEntity, IHeatConsumer
{
    private static final Direction[] SLOT_DIRECTIONS = {Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST};

    public static void serverTick(Level level, BlockPos pos, BlockState state, GlassworkingWheelBlockEntity wheel)
    {
        wheel.checkForLastTickSync();
        if (level.getGameTime() % 5 == 0 && !wheel.isInvalidInNetwork())
        {
            final float angle = wheel.getRotationAngle(0f);
            final int baseOffset = Math.round(angle / Mth.HALF_PI);

            for (int d = 0; d < 4; d++)
            {
                final int slot = Math.floorMod(d + baseOffset, 4);
                final BlockPos relPos = pos.relative(SLOT_DIRECTIONS[d]);
                final ItemStack stack = wheel.inventory.getStackInSlot(slot);
                if (stack.isEmpty())
                    continue;
                final BlockState relState = level.getBlockState(relPos);
                if (!relState.isAir() && !Helpers.isBlock(relState, TFCTags.Blocks.GLASSWORKING_WHEEL_INVISIBLE))
                {
                    Helpers.spawnItem(level, relPos, wheel.inventory.extractItem(slot, 1, false), 0.5f, 0, 0, 0);
                    wheel.markForSync();
                }
                if (relState.getBlock() == TFCBlocks.HATCH.get())
                {
                    if (HatchBlock.interactWithBlowpipe(level, relPos, stack))
                        wheel.markForSync();
                }
            }
        }
    }

    public static final int SLOTS = 4;

    private final Node node;

    public GlassworkingWheelBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCBlockEntities.GLASSWORKING_WHEEL.get(), pos, state, defaultInventory(SLOTS));
        this.node = new SinkNode(pos, Direction.UP) {
            @Override
            public String toString()
            {
                return "GlassworkingWheel[pos=%s]".formatted(pos());
            }
        };
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return Helpers.isItem(stack, TFCTags.Items.BLOWPIPES);
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return 1;
    }

    @Override
    public float getTemperature()
    {
        return 0;
    }

    @Override
    public void setTemperature(float temperature)
    {
        if (temperature > 0f)
        {
            for (ItemStack stack : Helpers.iterate(inventory))
            {
                final IHeat heat = HeatCapability.get(stack);
                if (heat != null)
                {
                    HeatCapability.addTemp(heat, temperature);
                    markForSync();
                }
            }
        }
    }

    @Override
    protected void onLoadAdditional()
    {
        performNetworkAction(NetworkAction.ADD);
    }

    @Override
    protected void onUnloadAdditional()
    {
        performNetworkAction(NetworkAction.REMOVE);
    }

    @Override
    public Node getRotationNode()
    {
        return node;
    }
}
