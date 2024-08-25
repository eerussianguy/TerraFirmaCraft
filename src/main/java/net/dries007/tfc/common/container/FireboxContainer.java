package net.dries007.tfc.common.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.dries007.tfc.common.blockentities.FireboxBlockEntity;
import net.dries007.tfc.common.container.slot.CallbackSlot;

public class FireboxContainer extends BlockEntityContainer<FireboxBlockEntity>
{
    public static FireboxContainer create(FireboxBlockEntity firebox, Inventory playerInventory, int windowId)
    {
        return new FireboxContainer(firebox, windowId).init(playerInventory, 20);
    }

    public FireboxContainer(FireboxBlockEntity blockEntity, int windowId)
    {
        super(TFCContainerTypes.FIREBOX.get(), windowId, blockEntity);
    }

    @Override
    protected boolean moveStack(ItemStack stack, int slotIndex)
    {
        return switch (typeOf(slotIndex))
        {
            case MAIN_INVENTORY, HOTBAR -> !moveItemStackTo(stack, 0, FireboxBlockEntity.SLOTS, false);
            case CONTAINER -> !moveItemStackTo(stack, containerSlots, slots.size(), false);
        };
    }

    @Override
    protected void addContainerSlots()
    {
        for (int i = 0; i < FireboxBlockEntity.SLOTS; i++)
        {
            addSlot(new CallbackSlot(blockEntity, i, 18 * (i % 4) + 62, 18 * (i / 4) + 19));
        }
    }
}
