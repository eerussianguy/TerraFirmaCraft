/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blockentities;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.tuple.Pair;

import net.dries007.tfc.client.model.MoldTableBlockModel;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.capabilities.DelegateItemHandler;
import net.dries007.tfc.common.capabilities.InventoryItemHandler;
import net.dries007.tfc.common.capabilities.ItemCapabilities;
import net.dries007.tfc.common.capabilities.PartialFluidHandler;
import net.dries007.tfc.common.capabilities.PartialItemHandler;
import net.dries007.tfc.common.capabilities.SidedHandler;
import net.dries007.tfc.common.component.heat.IHeat;
import net.dries007.tfc.common.component.heat.IHeatConsumer;
import net.dries007.tfc.common.component.heat.IHeatView;
import net.dries007.tfc.common.component.mold.IMold;
import net.dries007.tfc.common.recipes.CastingRecipe;
import net.dries007.tfc.common.recipes.InstantBarrelRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.util.ChannelFlow;
import net.dries007.tfc.util.FluidAlloy;
import net.dries007.tfc.util.Helpers;

public class MoldTableBlockEntity extends TickableInventoryBlockEntity<MoldTableBlockEntity.MoldBlockInventory>
{
    public static void serverTick(Level level, BlockPos pos, BlockState state, MoldTableBlockEntity mold)
    {
        mold.checkForLastTickSync();

        // If output is already present, do not move or try to draw liquids
        if (!mold.getOutputStack().isEmpty())
        {
            if (mold.hasSource())
            {
                mold.finishFlow();
            }
            return;
        }

        // Try to draw from crucible
        if (mold.hasSource() && level.getGameTime() % 2 == 0) // Draw at half the speed
        {
            level.getBlockEntity(mold.sourcePosition.get(), TFCBlockEntities.CRUCIBLE.get()).ifPresent(
                crucible -> {
                    // getFluidHandlerIfAppropriate ensures that the crucible's metal is the same as
                    // the one we
                    // we're expecting (mold.fluid).
                    // This ensures that we are not drawing two different fluids in the same "flow".
                    Optional<IFluidHandler> fluidHandler = getFluidHandlerIfAppropriate(crucible, mold.fluid);

                    // If the chain of channels was broken, finish the flow
                    if (fluidHandler.isEmpty() || !mold.isFlowing())
                    {
                        mold.finishFlow();
                        return;
                    }

                    final FluidStack outputDrop = fluidHandler.get().drain(1, IFluidHandler.FluidAction.SIMULATE);
                    final FluidStack outputRemainder = Helpers.mergeOutputFluidIntoSlot(
                        mold.getInventory(), outputDrop, crucible.getTemperature(), MoldTableBlockEntity.MOLD_SLOT);

                    Optional.ofNullable(mold.getMoldStack().getCapability(ItemCapabilities.HEAT))
                        .ifPresent(heatCap -> heatCap.setTemperature(crucible.getTemperature()));

                    if (outputRemainder.isEmpty())
                    {
                        // Remainder was emptied, so do the extraction for real
                        fluidHandler.get().drain(1, IFluidHandler.FluidAction.EXECUTE);
                        crucible.markForSync();
                        mold.markForSync();
                    }
                    else
                    {
                        // Could not fill any longer, finish the flow
                        mold.finishFlow();
                    }
                });
        }

        // Move results from mold item to output stack
        final ItemStack drainStack = mold.inventory.getStackInSlot(MOLD_SLOT);
        IMold moldItem = IMold.get(drainStack);
        if (moldItem != null && !moldItem.isMolten())
        {
            final CastingRecipe recipe = CastingRecipe.get(moldItem);
            if (recipe != null)
            {
                Optional.of(recipe.assemble(moldItem)).ifPresent(
                    stack -> {
                        mold.inventory.setStackInSlot(OUTPUT_SLOT, stack);
                        moldItem.drainIgnoringTemperature(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
                    });
            }
        }
    }

    /***
     * Returns the crucible's IFluidHandler only if:
     * - The crucible is not empty
     * - The fluid in the crucible is the same as shouldBeFluid, if given
     * - The metal is molten
     */
    public static Optional<IFluidHandler> getFluidHandlerIfAppropriate(CrucibleBlockEntity crucible, Optional<Fluid> shouldBeFluid)
    {
        final IFluidHandler crucibleFluidHandler = crucible.getSidedFluidInventory(Direction.NORTH);
        FluidAlloy alloy = crucible.getAlloy();

        // The crucible has no alloy
        if (alloy.isEmpty())
            return Optional.empty();

        // The fluid type of the alloy is not the same as the target fluid type
        if (shouldBeFluid.isPresent()
            && (alloy.getResult().getFluidType() != shouldBeFluid.get().getFluidType()))
        {
            return Optional.empty();
        }

        // The alloy is solidified
        if (!crucible.getInventory().isMolten())
        {
            return Optional.empty();
        }

        return Optional.ofNullable(crucibleFluidHandler);
    }

    /***
     * The position of the crucible where the flow is coming from
     *
     * Empty if the mold table does not have a flow currently
     */
    private Optional<BlockPos> sourcePosition = Optional.empty();

    /***
     * The direction where the flow is coming from,
     * as well as the distance (for downwards flow)
     *
     * Empty if the channel does not have a flow currently.
     */
    private Optional<Pair<Direction, Byte>> flowSource = Optional.empty();

    /***
     * The fluid to expect from the crucible. Flow should finish
     * if the fluid from the crucible is different than this value.
     *
     * Empty if the mold table does not have a flow currently
     */
    private Optional<Fluid> fluid = Optional.empty();

    private final SidedHandler<IFluidHandler> sidedFluidInventory;

    public static final int MOLD_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    public MoldTableBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCBlockEntities.MOLD_TABLE.get(), pos, state, MoldBlockInventory::new);

        sidedFluidInventory = new SidedHandler<>(inventory);

        sidedFluidInventory.on(
            PartialFluidHandler::insertOnly, // Allow input fluid from all sides and top
            Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.UP);

        PartialItemHandler handler = new PartialItemHandler(inventory);
        sidedInventory.on(handler.insert(MOLD_SLOT), Direction.UP)
            .on(handler.extract(OUTPUT_SLOT), Direction.DOWN);
    }

    @Nullable
    public IFluidHandler getSidedFluidInventory(@Nullable Direction context)
    {
        return sidedFluidInventory.get(context);
    }

    public boolean hasSource()
    {
        return sourcePosition.isPresent();
    }

    public void finishFlow()
    {
        assert level != null;
        flowSource.ifPresent(
            flowSource -> {
                Direction expectedDirection = flowSource.getLeft();
                int expectedDistance = flowSource.getRight();
                BlockPos expectedSourcePos = worldPosition.relative(expectedDirection, expectedDistance);

                Optional<ChannelBlockEntity> channelBE = level.getBlockEntity(expectedSourcePos,
                    TFCBlockEntities.CHANNEL.get());
                channelBE.ifPresent(channel -> channel.notifyBrokenLink(1));

                markForSync();
                sourcePosition = Optional.empty();
                fluid = Optional.empty();
                this.flowSource = Optional.empty();
            });
    }

    public void setSource(BlockPos sourcePos, Fluid fluid, Pair<Direction, Byte> flowSource)
    {
        this.sourcePosition = Optional.of(sourcePos);
        this.fluid = Optional.of(fluid);
        this.flowSource = Optional.of(flowSource);
    }

    public boolean isFlowing()
    {
        assert level != null;
        Optional<ChannelBlockEntity> blockEntity = level.getBlockEntity(
            worldPosition.relative(flowSource.get().getLeft(), flowSource.get().getRight()),
            TFCBlockEntities.CHANNEL.get());

        if (blockEntity.isEmpty())
            return false;

        for (byte i = 1; i < flowSource.get().getRight(); i++)
        {
            BlockPos rel = worldPosition.relative(flowSource.get().getLeft(), i);
            if (!level.getBlockState(rel).isAir())
            {
                return false;
            }
        }

        if (blockEntity.get().isFlowing())
        {
            if (flowSource.get().getLeft() == Direction.UP && flowSource.get().getRight() > 1)
                ChannelFlow.damageEntities(level, worldPosition, flowSource.get().getRight());
            return true;
        }
        return false;
    }

    @Override
    public ModelData getModelData()
    {
        return ModelData.of(MoldTableBlockModel.MoldModelData.PROPERTY, MoldTableBlockModel.getMoldModelData(getMoldStack()));
    }

    public Fluid getFluidToRender()
    {
        return fluid.orElseThrow();
    }

    public Pair<Direction, Byte> getFlowSource()
    {
        return flowSource.orElseThrow();
    }

    public ItemStack getMoldStack()
    {
        return inventory.getStackInSlot(MOLD_SLOT);
    }

    public ItemStack getOutputStack()
    {
        return inventory.getStackInSlot(OUTPUT_SLOT);
    }

    public void setMoldStack(ItemStack stack)
    {
        inventory.setStackInSlot(MOLD_SLOT, stack);
    }

    public void setOutputStack(ItemStack stack)
    {
        inventory.setStackInSlot(OUTPUT_SLOT, stack);
    }

    public ItemInteractionResult onRightClick(Player player)
    {
        assert level != null;
        final boolean interactWithMoldSlot = player.isShiftKeyDown() || inventory.getStackInSlot(MOLD_SLOT).isEmpty();

        if (interactWithMoldSlot)
        {
            final ItemStack heldItem = player.getMainHandItem();
            final boolean shouldExtract = !inventory.getStackInSlot(MOLD_SLOT).isEmpty();
            final boolean shouldInsert = !heldItem.isEmpty() && isItemValid(MOLD_SLOT, heldItem);
            if (shouldExtract)
            {
                // Swap items
                if (shouldInsert)
                {
                    final ItemStack extracted = inventory.extractItem(MOLD_SLOT, 1, false);
                    inventory.insertItem(MOLD_SLOT, heldItem.split(1), false);
                    ItemHandlerHelper.giveItemToPlayer(player, extracted, player.getInventory().selected);
                }
                else
                {
                    // Just extract
                    ItemHandlerHelper.giveItemToPlayer(player, inventory.extractItem(MOLD_SLOT, 1, false),
                        player.getInventory().selected);

                }

                final ItemStack extracted = inventory.extractItem(OUTPUT_SLOT, 99, false);
                ItemHandlerHelper.giveItemToPlayer(player, extracted, player.getInventory().selected);

                markForSync();
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            else if (shouldInsert)
            {
                inventory.insertItem(MOLD_SLOT, heldItem.split(1), false);
                markForSync();
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        else
        {
            final boolean shouldExtract = !inventory.getStackInSlot(OUTPUT_SLOT).isEmpty();
            if (shouldExtract)
            {
                ItemHandlerHelper.giveItemToPlayer(player, inventory.extractItem(OUTPUT_SLOT, 1, false),
                    player.getInventory().selected);

                markForSync();
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        if (slot == MOLD_SLOT)
        {
            return Helpers.isItem(stack, TFCTags.Items.USABLE_IN_MOLD_TABLE);
        }

        return true;
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return 1;
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        if (nbt.contains("sourcePosition"))
        {
            sourcePosition = Optional.of(BlockPos.of(nbt.getLong("sourcePosition")));
            flowSource = Optional.of(
                Pair.of(
                    Helpers.DIRECTIONS[nbt.getByte("flowSource")],
                    nbt.contains("flowSourceDistance") ? nbt.getByte("flowSourceDistance") : 1));
            fluid = Optional.of(BuiltInRegistries.FLUID.get(ResourceLocation.parse(nbt.getString("fluid"))));
        }
        else
        {
            sourcePosition = Optional.empty();
            flowSource = Optional.empty();
            fluid = Optional.empty();
        }
        super.loadAdditional(nbt, provider);
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        if (hasSource())
        {
            nbt.putLong("sourcePosition", sourcePosition.orElseThrow().asLong());
            nbt.putByte("flowSource", (byte) flowSource.orElseThrow().getLeft().ordinal());
            nbt.putByte("flowSourceDistance", flowSource.get().getRight());
            nbt.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid.orElseThrow()).toString());
        }
        super.saveAdditional(nbt, provider);
    }

    public void intakeAir(int amount)
    {
        final ItemStack drainStack = this.inventory.getStackInSlot(MOLD_SLOT);
        Optional.ofNullable(IMold.get(drainStack)).ifPresent(
            moldItem -> {
                moldItem.setTemperature(Math.max(0, moldItem.getTemperature() - amount / 4f));
            });
    }

    @Override
    public void setAndUpdateSlots(int slot)
    {
        super.setAndUpdateSlots(slot);
        requestModelDataUpdate();
        if (level != null && level.isClientSide)
        {
            // Need to make sure that this gets called at least on the client
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        }
    }

    /**
     * This inventory delegates the fluid and heat handler to the
     * item in the mold stack, as long as this item has the
     * corresponding capabilities (i.e. a mold item is present).
     * If it does not have them, it implements some default behaviour for every method.
     * Moreover, it adds some custom behaviour for fill
     */
    public static class MoldBlockInventory implements IFluidHandler, IHeatConsumer, DelegateItemHandler, INBTSerializable<CompoundTag>
    {
        private final MoldTableBlockEntity moldTable;
        private final InventoryItemHandler inventory;

        MoldBlockInventory(InventoryBlockEntity<?> entity)
        {
            moldTable = (MoldTableBlockEntity) entity;
            inventory = new InventoryItemHandler(entity, 2);
        }

        @Override
        public IItemHandlerModifiable getItemHandler()
        {
            return inventory;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider)
        {
            final CompoundTag nbt = new CompoundTag();
            nbt.put("inventory", inventory.serializeNBT(provider));
            return nbt;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)
        {
            inventory.deserializeNBT(provider, nbt.getCompound("inventory"));
        }

        private Optional<IFluidHandler> getMoldFluidHandler()
        {
            return Optional.ofNullable(moldTable.getMoldStack().getCapability(ItemCapabilities.FLUID));
        }

        private Optional<IHeat> getMoldHeatHandler()
        {
            return Optional.ofNullable(moldTable.getMoldStack().getCapability(ItemCapabilities.HEAT));
        }

        @Override
        public float getTemperature()
        {
            return getMoldHeatHandler().map(IHeatView::getTemperature).orElse(0f);
        }

        @Override
        public void setTemperature(float temp)
        {
            getMoldHeatHandler().ifPresent(h -> h.setTemperature(temp));
        }

        @Override
        public int getTanks()
        {
            return getMoldFluidHandler().map(IFluidHandler::getTanks).orElse(0);
        }

        @Override
        public FluidStack getFluidInTank(int tank)
        {
            return getMoldFluidHandler().map(h -> h.getFluidInTank(tank)).orElse(FluidStack.EMPTY.copy());
        }

        @Override
        public int getTankCapacity(int tank)
        {
            return getMoldFluidHandler().map(h -> h.getTankCapacity(tank)).orElse(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack)
        {
            return getMoldFluidHandler().map(h -> h.isFluidValid(tank, stack)).orElse(false);
        }

        @Override
        public int fill(FluidStack fluid, FluidAction action)
        {
            // if mold or output are present, try to apply instantaneous barrel recipes
            // Essentially this allows cooling down the molds,
            // although it accepts other recipes if added
            ItemStack stack;
            boolean usedMoldStack;

            if (!moldTable.getOutputStack().isEmpty())
            {
                stack = moldTable.getOutputStack();
                usedMoldStack = false;
            }
            else
            {
                stack = moldTable.getMoldStack();
                usedMoldStack = true;
            }

            Level level = moldTable.getLevel();

            if (!stack.isEmpty() && level != null)
            {
                final RecipeManager recipeManager = level.getRecipeManager();

                // If there is an instant barrel recipe with the mold stack
                // and the filled liquid, then this returns a Pair with the
                // result ItemStack and the amount of fluid that would be consumed
                Optional<Pair<ItemStack, Integer>> recipeResult = recipeManager
                    .getAllRecipesFor(
                        TFCRecipeTypes.BARREL_INSTANT.get())
                    .stream()
                    .filter(
                        recipeHolder -> recipeHolder.value().getInputItem().test(stack)
                            && recipeHolder.value().getInputFluid().test(fluid))
                    .findFirst().map(
                        recipeHolder -> {
                            InstantBarrelRecipe recipe = recipeHolder.value();
                            SizedIngredient inputItem = recipe.getInputItem();
                            SizedFluidIngredient inputFluid = recipe.getInputFluid();

                            // Evaluates an instant barrel recipe, similar to InstantFluidBarrelRecipe#assembleOutputs

                            // Calculate the multiplier in use for this recipe
                            int multiplier;
                            if (inputItem.count() == 0)
                            {
                                multiplier = fluid.getAmount() / inputFluid.amount();
                            }
                            else if (inputFluid.amount() == 0)
                            {
                                multiplier = stack.getCount() / inputItem.count();
                            }
                            else
                            {
                                multiplier = Math.min(fluid.getAmount() / inputFluid.amount(),
                                    stack.getCount() / inputItem.count());
                            }

                            // Compute output
                            final ItemStack outputItem = recipe.getOutputItem().getSingleStack(stack);
                            if (!outputItem.isEmpty())
                            {
                                outputItem.setCount(Math.min(outputItem.getMaxStackSize(),
                                    multiplier * outputItem.getCount()));
                            }

                            // Amount of fluid that would be consumed
                            return Pair.of(outputItem, multiplier * inputFluid.amount());
                        });

                if (recipeResult.isPresent())
                {
                    if (action == FluidAction.EXECUTE)
                    {
                        if (usedMoldStack)
                        {
                            moldTable.setMoldStack(recipeResult.get().getLeft());
                        }
                        else
                        {
                            moldTable.setOutputStack(recipeResult.get().getLeft());
                        }
                    }
                    return recipeResult.get().getRight();
                }
            }

            return getMoldFluidHandler().map(h -> h.fill(fluid, action)).orElse(0);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action)
        {
            return getMoldFluidHandler().map(h -> h.drain(resource, action)).orElse(FluidStack.EMPTY.copy());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action)
        {
            return getMoldFluidHandler().map(h -> h.drain(maxDrain, action)).orElse(FluidStack.EMPTY.copy());
        }
    }
}
