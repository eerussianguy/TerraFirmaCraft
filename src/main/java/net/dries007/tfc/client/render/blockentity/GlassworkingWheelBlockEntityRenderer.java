package net.dries007.tfc.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.blockentities.rotation.GlassworkingWheelBlockEntity;

public class GlassworkingWheelBlockEntityRenderer implements BlockEntityRenderer<GlassworkingWheelBlockEntity>
{
    public static final ModelResourceLocation AXLE_MODEL = RenderHelpers.modelId("block/glassworking_wheel");

    @Override
    public void render(GlassworkingWheelBlockEntity wheel, float partialTick, PoseStack stack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        final Level level = wheel.getLevel();
        if (level == null)
            return;

        final float angle = -wheel.getRotationAngle(partialTick);
        final RandomSource random = RandomSource.create();
        final VertexConsumer cutout = buffer.getBuffer(RenderType.cutout());

        stack.pushPose();
        stack.translate(0.5f, 0.5f, 0.5f);
        stack.mulPose(Axis.YP.rotation(angle));
        stack.translate(-0.5f, -0.5f, -0.5f);

        final ModelBlockRenderer modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        final BakedModel baked = Minecraft.getInstance().getModelManager().getModel(AXLE_MODEL);

        modelRenderer.tesselateWithAO(level, baked, wheel.getBlockState(), wheel.getBlockPos(), stack, cutout, true, random, packedLight, packedOverlay, ModelData.EMPTY, RenderType.cutout());

        stack.popPose();

        for (int i = 0; i < GlassworkingWheelBlockEntity.SLOTS; i++)
        {
            final ItemStack item = wheel.getInventory().getStackInSlot(i);
            if (!item.isEmpty())
            {
                stack.pushPose();
                stack.translate(0.5f, 0.5f, 0.5f);
                stack.mulPose(Axis.YP.rotation(angle + Mth.HALF_PI * i));

                stack.translate(0f, 0f, 0.25f);

                stack.scale(1 / 1.6f, 1 / 1.6f, 1 / 1.6f);
                Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.HEAD, packedLight, packedOverlay, stack, buffer, level, 0);

                stack.popPose();
            }
        }
    }
}