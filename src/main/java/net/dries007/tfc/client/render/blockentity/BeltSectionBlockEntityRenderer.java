package net.dries007.tfc.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.blockentities.rotation.BeltSectionBlockEntity;
import net.dries007.tfc.common.blocks.rotation.BeltSectionBlock;
import net.dries007.tfc.util.rotation.Node;
import net.dries007.tfc.util.rotation.Rotation;

public class BeltSectionBlockEntityRenderer implements BlockEntityRenderer<BeltSectionBlockEntity>
{

    @Override
    public void render(BeltSectionBlockEntity belt, float partialTick, PoseStack pose, MultiBufferSource bufferSource, int packedLight, int packedOverlay)
    {
        final BlockState state = belt.getBlockState();
        final Level level = belt.getLevel();
        if (!(state.getBlock() instanceof BeltSectionBlock block) || level == null)
        {
            return;
        }

        final Node node = belt.getRotationNode();
        final Rotation rotation = node.rotation();
        final float angle = rotation != null ? rotation.angle(partialTick) : 0f;
        final float firstHalfPixels = Mth.abs(angle) / Mth.TWO_PI;

        final TextureAtlasSprite sprite = RenderHelpers.blockTexture(BeltBlockEntityRenderer.TEXTURE);
        final VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());

        pose.pushPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90f));
        pose.popPose();
        RenderHelpers.renderTexturedCuboid(pose, buffer, sprite, packedLight, packedOverlay, 0f, 12 / 16f, 2 / 16f, firstHalfPixels, 13 / 16f, 14 / 16f);
        BeltBlockEntityRenderer.flipPose(pose, firstHalfPixels);
        RenderHelpers.renderTexturedCuboid(pose, buffer, sprite, packedLight, packedOverlay, firstHalfPixels, 12 / 16f, 2 / 16f, 1f, 13 / 16f, 14 / 16f);

        pose.popPose();
    }

}
