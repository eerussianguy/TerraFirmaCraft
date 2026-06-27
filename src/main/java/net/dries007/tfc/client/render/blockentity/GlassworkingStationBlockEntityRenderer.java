/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.dries007.tfc.common.blockentities.GlassworkingStationBlockEntity;
import net.dries007.tfc.common.blocks.GlassworkingStationBlock;

public class GlassworkingStationBlockEntityRenderer implements BlockEntityRenderer<GlassworkingStationBlockEntity>
{
    @Override
    public void render(GlassworkingStationBlockEntity station, float partialTick, PoseStack stack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        final Level level = station.getLevel();
        if (level == null)
            return;

        final ItemStack tool = station.getInventory().getStackInSlot(0);
        if (tool.isEmpty())
            return;

        // The block's front (the mounted tool / working end) points along FACING. Orient the rendered tool to match.
        final float facingAngle = station.getBlockState().getValue(GlassworkingStationBlock.FACING).toYRot();

        stack.pushPose();

        // Position the tool above the station body. The block model occupies y=0..12, so sit the item on top of it.
        // TODO tune these transforms once the block model exists.
        stack.translate(0.5f, 6.5f / 16f, 0.5f);
        stack.mulPose(Axis.YP.rotationDegrees(facingAngle));
        stack.mulPose(Axis.XP.rotationDegrees(90f)); // lay the tool flat, facing up
        stack.scale(0.75f, 0.75f, 0.75f);

        Minecraft.getInstance().getItemRenderer().renderStatic(tool, ItemDisplayContext.FIXED, packedLight, packedOverlay, stack, buffer, level, 0);

        stack.popPose();
    }
}
