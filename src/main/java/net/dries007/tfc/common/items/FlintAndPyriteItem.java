/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.items;

import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.events.StartFireEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class FlintAndPyriteItem extends Item
{
    public FlintAndPyriteItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        final Player player = context.getPlayer();
        final Level level = context.getLevel();
        final BlockHitResult result = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        final ItemStack stack = context.getItemInHand();

        final BlockPos pos = result.getBlockPos();
        final BlockPos abovePos = pos.above();
        if (player != null)
        {
            if (!player.isCreative())
            {
                Helpers.damageItem(stack, player, InteractionHand.MAIN_HAND);
            }
            level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            final double chance = TFCConfig.SERVER.flintAndPyriteChance.get();
            // If the following is run on the client, it will sometimes get out of sync with the server leading to visual glitches including invisible items when starting a campfire
            if (level.random.nextFloat() < chance && !level.isClientSide)
            {
                StartFireEvent.startFire(level, pos, level.getBlockState(pos), result.getDirection(), player, stack, StartFireEvent.FireStrength.STRONG, level.isRainingAt(abovePos) ? 0.3 : 1 * chance);
            }
            else
            {
                StartFireEvent.startFire(level, pos, level.getBlockState(pos), result.getDirection(), player, stack, StartFireEvent.FireStrength.WEAK, level.isRainingAt(abovePos) ? 0.3 : 1 * chance);
            }
        }
        return InteractionResult.CONSUME;
    }
}
