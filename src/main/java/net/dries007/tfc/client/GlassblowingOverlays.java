/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client;


import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.GlassBasinBlock;
import net.dries007.tfc.common.component.glass.GlassOperation;
import net.dries007.tfc.common.component.heat.Heat;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.util.Helpers;

import static net.dries007.tfc.common.items.BlowpipeItem.*;


public class GlassblowingOverlays
{
    public static boolean render(Minecraft minecraft, GuiGraphics graphics)
    {
        final Level level = minecraft.level;
        final Player player = minecraft.player;
        final BlockPos targetedPos = ClientHelpers.getTargetedPos();
        final Direction targetedFace = ClientHelpers.getTargetedFace();
        final int x = graphics.guiWidth() / 2 + 3;
        final int y = graphics.guiHeight() / 2 + 8;

        if (player != null)
        {
            ItemStack held = player.getMainHandItem();
            ItemStack otherItem = player.getOffhandItem();

            if (!Helpers.isItem(player.getMainHandItem(), TFCTags.Items.GLASS_BLOWPIPES))
            {
                held = player.getOffhandItem();
                otherItem = player.getMainHandItem();
            }

            ChatFormatting color = ChatFormatting.WHITE;
            if (Heat.getHeat(HeatCapability.getTemperature(held)) != null)
            {
                color = Objects.requireNonNull(Heat.getHeat(HeatCapability.getTemperature(held))).getColor();
            }

            if (player.getCooldowns().isOnCooldown(held.getItem()))
            {
                Component line = Component.translatable("glass.tfc.complete");
                drawCenteredText(minecraft, graphics, line, x, y);
                return true;
            }

            if (level != null && targetedPos != null && targetedFace != null)
            {
                final BlockState targetedState = level.getBlockState(targetedPos);
                final BlockPos center = targetedPos.relative(targetedFace);
                if (GlassBasinBlock.isValid(level, center))
                {
                    Component line = Component.translatable(GlassOperation.BASIN_POUR.get().getTranslationId()).withStyle(color);
                    drawCenteredText(minecraft, graphics, line, x, y);
                    return true;
                }
                if (targetedFace == Direction.UP && Helpers.isBlock(targetedState, TFCTags.Blocks.GLASS_POURING_TABLE) && level.getBlockState(targetedPos.above()).isAir())
                {
                    Component line = Component.translatable(GlassOperation.TABLE_POUR.get().getTranslationId()).withStyle(color);
                    drawCenteredText(minecraft, graphics, line, x, y);
                    return true;
                }
            }

            final GlassOperation op = GlassOperation.get(otherItem, player);
            if (op != null)
            {
                StringBuilder progress = new StringBuilder();
                if (player.isUsingItem())
                {
                    int tally = (OPERATION_USE_DURATION - player.getUseItemRemainingTicks()) / 8 + 1;
                    progress.append("|".repeat(tally));
                }

                Component line = Component.translatable(op.getTranslationId()).append(" " + progress).withStyle(color);

                drawCenteredText(minecraft, graphics, line, x, y);
                return true;
            }
        }


        return false;
    }

    private static void drawCenteredText(Minecraft minecraft, GuiGraphics graphics, Component text, int x, int y)
    {
        final int textWidth = minecraft.font.width(text) / 2;
        graphics.drawString(minecraft.font, text, x - textWidth, y, 0xCCCCCC, true);
    }
}
