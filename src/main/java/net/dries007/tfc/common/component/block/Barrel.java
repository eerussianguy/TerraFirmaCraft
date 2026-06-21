/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.component.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.dries007.tfc.common.blockentities.BarrelBlockEntity;
import net.dries007.tfc.common.blockentities.BarrelBlockEntity.BarrelInventory;
import net.dries007.tfc.common.component.ComponentView;
import net.dries007.tfc.common.component.TFCComponents;
import net.dries007.tfc.common.component.fluid.FluidComponent;
import net.dries007.tfc.common.component.fluid.FluidContainer;
import net.dries007.tfc.common.component.fluid.FluidContainerInfo;
import net.dries007.tfc.common.recipes.RecipeHelpers;
import net.dries007.tfc.common.recipes.SealedBarrelRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.component.fluid.FluidComponent.DrainInfo;
import net.dries007.tfc.common.component.fluid.FluidComponent.FillInfo;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;

/**
 * A simple merged fluid handler for barrels, that respects the barrels {@link BarrelComponent#hasActiveRecipe()} - no modifications are
 * possible while that is present.
 */
public class Barrel extends ComponentView<BarrelComponent> implements FluidContainer, IFluidHandlerItem
{
    public Barrel(ItemStack stack)
    {
        super(stack, TFCComponents.BARREL, BarrelComponent.EMPTY);
    }

    @Override
    public ItemStack getContainer()
    {
        return stack;
    }

    @Override
    public FluidContainerInfo containerInfo()
    {
        return BarrelBlockEntity.BarrelInventory.INFO;
    }

    @Override
    public FluidStack getFluidInTank(int tank)
    {
        return component.fluidContent();
    }

    @Override
    public int fill(FluidStack input, FluidAction action)
    {
        if (component.hasActiveRecipe()) return 0; // No interaction with an active recipe

        final FillInfo result = FluidComponent.fill(component.fluidContent(), input, containerInfo());
        if (action.execute())
        {
            final BarrelInventory fakeInventory = new BarrelInventory(() -> false);
            fakeInventory.getItemHandler().setStackInSlot(BarrelBlockEntity.SLOT_ITEM, component.itemContent().getFirst());
            fakeInventory.getFluidHandler().fill(input, FluidAction.EXECUTE);

            final RecipeManager manager = Helpers.getUnsafeRecipeManager();
            final RecipeHolder<SealedBarrelRecipe> recipe = RecipeHelpers.getHolder(manager, TFCRecipeTypes.BARREL_SEALED, fakeInventory);

            apply(new BarrelComponent(
                component.itemContent(),
                result.content(),
                Calendars.get().getTicks(),
                recipe != null ? Calendars.get().getTicks() : component.recipeTick()
            ));
        }

        return result.filled();
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action)
    {
        if (component.hasActiveRecipe()) return FluidStack.EMPTY;

        final DrainInfo result = FluidComponent.drain(component.fluidContent(), maxDrain);
        if (action.execute()) apply(component.with(result.content()));

        return result.drained();
    }
}
