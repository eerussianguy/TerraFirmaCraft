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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.blockentities.rotation.BeltBlockEntity;
import net.dries007.tfc.common.blocks.rotation.BeltBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.rotation.Node;
import net.dries007.tfc.util.rotation.Rotation;

public class BeltBlockEntityRenderer implements BlockEntityRenderer<BeltBlockEntity>
{
    public static final ResourceLocation TEXTURE = Helpers.identifier("block/burlap");

    @Override
    public void render(BeltBlockEntity belt, float partialTick, PoseStack pose, MultiBufferSource bufferSource, int packedLight, int packedOverlay)
    {
        final BlockState state = belt.getBlockState();
        final Level level = belt.getLevel();
        if (!(state.getBlock() instanceof BeltBlock block) || level == null)
        {
            return;
        }
        final Direction.Axis axis = state.getValue(BeltBlock.AXIS);

        final Node node = belt.getRotationNode();
        final Rotation rotation = node.rotation();
        final float angle = rotation != null ? rotation.angle(partialTick) : 0f;
        final float firstHalfPixels = Mth.abs(angle) / Mth.TWO_PI;

        final TextureAtlasSprite sprite = RenderHelpers.blockTexture(TEXTURE);
        final VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());

        pose.pushPose();

        AxleBlockEntityRenderer.applyRotation(pose, axis, -belt.getRotationAngle(partialTick));
        AxleBlockEntityRenderer.renderAxle(pose, bufferSource, block, axis, packedLight, packedOverlay, -belt.getRotationAngle(partialTick));

        pose.popPose();
        pose.pushPose();

        RenderHelpers.renderTexturedCuboid(pose, buffer, sprite, packedLight, packedOverlay, 0f, 12 / 16f, 2 / 16f, firstHalfPixels, 13 / 16f, 14 / 16f);
        flipPose(pose, firstHalfPixels);
        RenderHelpers.renderTexturedCuboid(pose, buffer, sprite, packedLight, packedOverlay, firstHalfPixels, 12 / 16f, 2 / 16f, 1f, 13 / 16f, 14 / 16f);

        pose.popPose();
    }

    public static void flipPose(PoseStack pose, float amount)
    {
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        pose.translate(-0.5f, -0.5f, -0.5f);
        pose.translate(-amount, 0f, 0f);
    }
}
