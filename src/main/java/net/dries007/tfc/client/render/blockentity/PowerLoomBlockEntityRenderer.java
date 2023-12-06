package net.dries007.tfc.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.blockentities.rotation.PowerLoomBlockEntity;
import net.dries007.tfc.common.blocks.rotation.PowerLoomBlock;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.recipes.LoomRecipe;
import net.dries007.tfc.util.Helpers;

public class PowerLoomBlockEntityRenderer implements BlockEntityRenderer<PowerLoomBlockEntity>
{
    @Override
    public void render(PowerLoomBlockEntity loom, float partialTicks, PoseStack stack, MultiBufferSource buffers, int combinedLight, int combinedOverlay)
    {
        if (!(loom.getBlockState().getBlock() instanceof PowerLoomBlock loomBlock) || loom.getLevel() == null)
        {
            return;
        }
        final IItemHandler inv = Helpers.getCapability(loom, Capabilities.ITEM);
        if (inv == null)
        {
            return;
        }

        final LoomRecipe recipe = loom.getRecipe();
        final ResourceLocation lastTex = loom.getLastTexture();
        final Direction face = loom.getBlockState().getValue(PowerLoomBlock.FACING);
        final float px = 1f / 16f;
        if (recipe != null || lastTex != null)
        {
            final VertexConsumer buffer = buffers.getBuffer(RenderType.cutout());

            stack.translate(0.5f, 0.5f, 0.5f);
            stack.mulPose(Axis.YP.rotationDegrees(180f - 90f * face.get2DDataValue()));
            stack.translate(-0.5f, -0.5f, -0.5f);

            final ItemStack output = inv.getStackInSlot(PowerLoomBlockEntity.SLOT_OUTPUT);
            int simulatedCount = output.getCount();
            if (recipe != null)
            {
                simulatedCount += recipe.getResult().getEmptyStack().getCount() * (loom.getProgress() / (float) recipe.getStepCount());
            }
            final float percentFull = Math.min(simulatedCount / 64f, 1f);
            if (percentFull > 0f && lastTex != null)
            {
                // render output queue
                final TextureAtlasSprite product = RenderHelpers.blockTexture(lastTex);

                final float pct = Math.min(percentFull, 0.2f) / 0.2f;
                final int pixels = Mth.ceil(pct * 3);
                RenderHelpers.renderTexturedCuboid(stack, buffer, product, combinedLight, combinedOverlay, 2 * px, 9 * px, px * (4 - pixels), 14 * px, 10 * px, 4 * px);
                if (percentFull > 20f)
                {
                    final float pct2 = (percentFull - 0.2f) / 0.8f;
                    final int pixels2 = Mth.ceil(pct2 * 11);
                    RenderHelpers.renderTexturedCuboid(stack, buffer, product, combinedLight, combinedOverlay, 2 * px, (11 - pixels2) * px, 0, 14 * px, 11 * px, 1 * px);
                }
            }
            if (recipe != null)
            {
                final TextureAtlasSprite material = RenderHelpers.blockTexture(recipe.getInProgressTexture());

            }
        }
    }
}
