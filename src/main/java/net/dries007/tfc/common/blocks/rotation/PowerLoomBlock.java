package net.dries007.tfc.common.blocks.rotation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.dries007.tfc.common.blockentities.rotation.PowerLoomBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.devices.HorizontalDirectionalDeviceBlock;

public class PowerLoomBlock extends HorizontalDirectionalDeviceBlock
{
    public PowerLoomBlock(ExtendedProperties properties)
    {
        super(properties, InventoryRemoveBehavior.DROP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (level.getBlockEntity(pos) instanceof PowerLoomBlockEntity loom)
        {
            return loom.onRightClick(player);
        }
        return InteractionResult.PASS;
    }

}
