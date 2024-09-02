package net.dries007.tfc.common.blockentities;

import java.util.ArrayDeque;
import java.util.Queue;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.FireboxBlock;
import net.dries007.tfc.common.component.heat.Heat;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.IHeat;
import net.dries007.tfc.common.container.FireboxContainer;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendarTickable;
import net.dries007.tfc.util.data.Fuel;

public class FireboxBlockEntity extends TickableInventoryBlockEntity<ItemStackHandler> implements ICalendarTickable
{
    public static void serverTick(Level level, BlockPos pos, BlockState state, FireboxBlockEntity box)
    {
        box.checkForLastTickSync();
        box.checkForCalendarUpdate();

        if (state.getValue(FireboxBlock.LIT))
        {
            if (box.burnTicks > 0)
            {
                box.burnTicks -= box.airTicks > 0 ? 2 : 1; // Fuel burns twice as fast using bellows
            }
            if (box.burnTicks <= 0 && !box.consumeFuel())
            {
                box.extinguish(state);
            }
        }
        else if (box.burnTemperature > 0)
        {
            box.extinguish(state);
        }
        if (box.airTicks > 0)
        {
            box.airTicks--;
        }

        // Always update temperature / cooking, until the fire pit is not hot anymore
        if (box.temperature > 0 || box.burnTemperature > 0)
        {
            box.temperature = HeatCapability.adjustDeviceTemp(box.temperature, box.burnTemperature, box.airTicks, false);

            HeatCapability.provideHeatTo(level, pos.above(), Direction.DOWN, box.temperature);

            box.markForSync();
        }

        if (box.needsSlotUpdate)
        {
            box.cascadeFuelSlots();
        }

        if (level.getGameTime() % 200 == 0)
        {
            box.operableBlocks = floodfill(level, pos.above(), box);
        }
        performHeating(level, box, box.operableBlocks);
    }

    private static Object2IntMap<BlockPos> floodfill(Level level, BlockPos pos, FireboxBlockEntity firebox)
    {
        record Path(BlockPos pos, int cost) {}

        if (level.isClientSide || firebox.temperature <= 0f)
        {
            return new Object2IntOpenHashMap<>();
        }

        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        final Object2IntMap<BlockPos> filled = new Object2IntOpenHashMap<>();
        final Queue<Path> queue = new ArrayDeque<>();

        filled.put(pos, 0);
        queue.add(new Path(pos, 0));

        int maxCost = -1, capacity = firebox.getCapacity();

        while (!queue.isEmpty())
        {
            final Path current = queue.remove();

            capacity--;
            if (capacity >= 0 && current.cost > maxCost)
            {
                maxCost = current.cost;
            }
            if (capacity <= 0 && current.cost > maxCost)
            {
                break;
            }

            for (Direction direction : NOT_DOWN)
            {
                cursor.setWithOffset(current.pos, direction);
                if (!filled.containsKey(cursor))
                {
                    final BlockState state = level.getBlockState(cursor);
                    if (!isValidExterior(level, cursor, state, direction))
                    {
                        if (isValidInterior(state))
                        {
                            final BlockPos posNext = cursor.immutable();

                            queue.add(new Path(posNext, current.cost + 1));
                            filled.put(posNext, cursor.distManhattan(pos));
                        }
                        else
                        {
                            filled.clear();
                            break;
                        }
                    }
                }
            }
        }
        final Object2IntMap<BlockPos> filteredMap = new Object2IntOpenHashMap<>();
        filled.object2IntEntrySet().stream().filter(entry -> level.getBlockEntity(entry.getKey()) instanceof PlacedItemBlockEntity).forEach(entry -> filteredMap.put(entry.getKey(), entry.getIntValue()));
        return filteredMap;
    }

    private static void performHeating(Level level, FireboxBlockEntity firebox, Object2IntMap<BlockPos> filled)
    {
        filled.forEach((testPos, distance) -> {
            if (level.getBlockEntity(testPos) instanceof PlacedItemBlockEntity placedItem)
            {
                final IItemHandler inv = placedItem.getInventory();
                for (int i = 0; i < inv.getSlots(); i++)
                {
                    final ItemStack item = inv.getStackInSlot(i);
                    final IHeat heat = HeatCapability.get(item);
                    if (heat != null)
                    {
                        final float temp = firebox.temperature - Mth.clampedMap(distance, 0, 8, 0, firebox.temperature);
                        HeatCapability.addTemp(heat, temp);
                        if (level.getGameTime() % 20 == 0)
                        {
                            final HeatingRecipe recipe = HeatingRecipe.getRecipe(item);
                            if (recipe != null && recipe.matches(item) && recipe.isValidTemperature(heat.getTemperature()))
                            {
                                final ItemStack output = recipe.assembleItem(item);
                                item.setCount(0);
                                inv.insertItem(i, output, false);
                                placedItem.markForSync();
                            }
                        }
                    }
                }
        }
        });
    }

    private static boolean isValidInterior(BlockState state)
    {
        return !state.canOcclude() || Helpers.isBlock(state, TFCTags.Blocks.HEAT_PASSABLE);
    }

    private static boolean isValidExterior(Level level, BlockPos.MutableBlockPos cursor, BlockState state, Direction direction)
    {
        return (Helpers.isBlock(state, TFCTags.Blocks.HEAT_INSULATION) || (Helpers.isBlock(state, TFCTags.Blocks.HEAT_PASSABLE) && direction == Direction.UP)) && state.isFaceSturdy(level, cursor, direction.getOpposite());
    }

    public static final int SLOTS = 16;
    private static final Direction[] NOT_DOWN = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

    private long lastPlayerTick;
    private int burnTicks, airTicks;
    private float temperature, burnTemperature;
    private boolean needsSlotUpdate = true;
    private Object2IntMap<BlockPos> operableBlocks = new Object2IntOpenHashMap<>();

    public FireboxBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCBlockEntities.FIREBOX.get(), pos, state, defaultInventory(SLOTS));
        lastPlayerTick = Calendars.SERVER.getTicks();
        temperature = 0;
        burnTemperature = 0;
        burnTicks = 0;
        airTicks = 0;
    }

    /**
     * @return the total number of blocks that this firebox can heat.
     */
    private int getCapacity()
    {
        return (int) (temperature / Heat.maxVisibleTemperature() * 32);
    }

    public void extinguish(BlockState state)
    {
        assert level != null;
        level.setBlockAndUpdate(worldPosition, state.setValue(FireboxBlock.LIT, false));
        burnTicks = 0;
        burnTemperature = 0;
        temperature = 0;
        markForSync();
    }

    public void intakeAir(int amount)
    {
        airTicks += amount;
        if (airTicks > BellowsBlockEntity.MAX_DEVICE_AIR_TICKS)
        {
            airTicks = BellowsBlockEntity.MAX_DEVICE_AIR_TICKS;
        }
    }

    @Override
    public void onCalendarUpdate(long ticks)
    {
        assert level != null;
        final BlockState state = level.getBlockState(worldPosition);
        if (state.getValue(FireboxBlock.LIT))
        {
            HeatCapability.Remainder remainder = HeatCapability.consumeFuelForTicks(ticks, inventory, burnTicks, burnTemperature, 0, SLOTS - 1);

            burnTicks = remainder.burnTicks();
            burnTemperature = remainder.burnTemperature();
            needsSlotUpdate = true;

            if (remainder.ticks() > 0)
            {
                // Consumed all fuel, so extinguish and cool instantly
                extinguish(state);
            }
        }
    }

    public boolean light(BlockState state)
    {
        assert level != null;
        if (burnTicks > 0)
        {
            return true; // Already lit
        }
        if (consumeFuel())
        {
            level.setBlockAndUpdate(worldPosition, state.setValue(FireboxBlock.LIT, true));
            return true;
        }
        return false;
    }


    /**
     * Attempts to consume one piece of fuel. Returns if the fire pit consumed any fuel (and so, ended up lit)
     */
    private boolean consumeFuel()
    {
        final ItemStack fuelStack = inventory.getStackInSlot(0);
        if (!fuelStack.isEmpty())
        {
            // Try and consume a piece of fuel
            inventory.setStackInSlot(0, ItemStack.EMPTY);
            needsSlotUpdate = true;
            final Fuel fuel = Fuel.get(fuelStack);
            if (fuel != null)
            {
                burnTicks += fuel.duration();
                burnTemperature = fuel.temperature();
            }
            markForSync();
        }
        return burnTicks > 0;
    }

    public float getTemperature()
    {
        return temperature;
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        temperature = nbt.getFloat("temperature");
        burnTicks = nbt.getInt("burnTicks");
        airTicks = nbt.getInt("airTicks");
        burnTemperature = nbt.getFloat("burnTemperature");
        lastPlayerTick = nbt.getLong("lastPlayerTick");
        super.loadAdditional(nbt, provider);
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        nbt.putFloat("temperature", temperature);
        nbt.putInt("burnTicks", burnTicks);
        nbt.putInt("airTicks", airTicks);
        nbt.putFloat("burnTemperature", burnTemperature);
        nbt.putLong("lastPlayerTick", lastPlayerTick);
        super.saveAdditional(nbt, provider);
    }

    @Override
    public void setAndUpdateSlots(int slot)
    {
        super.setAndUpdateSlots(slot);
        needsSlotUpdate = true;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return Fuel.get(stack) != null;
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return 1;
    }

    @Override
    @Deprecated
    public long getLastCalendarUpdateTick()
    {
        return lastPlayerTick;
    }

    @Override
    @Deprecated
    public void setLastCalendarUpdateTick(long tick)
    {
        lastPlayerTick = tick;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowID, Inventory playerInv, Player player)
    {
        return FireboxContainer.create(this, playerInv, windowID);
    }

    private void cascadeFuelSlots()
    {
        // This will cascade all fuel down to the lowest available slot
        int lowestAvailSlot = 0;
        for (int i = 0; i < SLOTS; i++)
        {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty())
            {
                // Move to lowest available slot
                if (i > lowestAvailSlot)
                {
                    inventory.setStackInSlot(lowestAvailSlot, stack.copy());
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
                lowestAvailSlot++;
            }
        }
        needsSlotUpdate = false;
    }

}
