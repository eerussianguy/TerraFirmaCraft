package net.dries007.tfc.client.render;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.dries007.tfc.client.model.IsopodModel;
import net.dries007.tfc.common.entities.aquatic.SeafloorCritterEntity;

import static net.dries007.tfc.TerraFirmaCraft.MOD_ID;

public class IsopodRenderer extends MobRenderer<SeafloorCritterEntity, IsopodModel>
{
    private static final ResourceLocation ISOPOD_LOCATION = new ResourceLocation(MOD_ID, "textures/entity/animal/isopod.png");

    public IsopodRenderer(EntityRendererManager manager)
    {
        super(manager, new IsopodModel(), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(SeafloorCritterEntity entityIn)
    {
        return ISOPOD_LOCATION;
    }

    @Override
    protected void setupRotations(SeafloorCritterEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks, float rotationYaw, float partialTicks)
    {
        super.setupRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(180F));
    }
}
