package net.dries007.tfc.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.rotation.AxleBlock;
import net.dries007.tfc.common.blocks.rotation.BeltBlock;

public class BeltBlockItem extends BlockItem
{
    public BeltBlockItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = level.getBlockState(pos);
        final Player player = context.getPlayer();

        if (state.getBlock() instanceof AxleBlock axle && state.getValue(AxleBlock.AXIS) != Direction.Axis.Y)
        {
            final Direction.Axis axis = state.getValue(AxleBlock.AXIS); // Don't move this outside the if() statement! It needs to check for an axle block first!

            level.setBlockAndUpdate(pos, axle.getBelt().get().defaultBlockState().setValue(BeltBlock.AXIS, axis));

            if (player == null || !player.isCreative())
            {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }
}
