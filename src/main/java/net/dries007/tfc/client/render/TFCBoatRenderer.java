package net.dries007.tfc.client.render;

import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.util.ResourceLocation;

import net.dries007.tfc.common.entities.TFCBoatEntity;
import net.dries007.tfc.common.types.Wood;

import static net.dries007.tfc.TerraFirmaCraft.MOD_ID;

public class TFCBoatRenderer extends BoatRenderer
{
    public TFCBoatRenderer(EntityRendererManager renderManagerIn)
    {
        super(renderManagerIn);
    }

    @Override
    public ResourceLocation getTextureLocation(BoatEntity entityIn)
    {
        if (entityIn instanceof TFCBoatEntity)
        {
            TFCBoatEntity tfcBoat = (TFCBoatEntity) entityIn;
            final Wood.Default wood = Wood.Default.valueOf(tfcBoat.getEntityData().get(TFCBoatEntity.TFC_WOOD_ID));
            return new ResourceLocation(MOD_ID, "textures/entity/boat/" + wood.name().toLowerCase() + ".png");
        }
        return super.getTextureLocation(entityIn);
    }
}
