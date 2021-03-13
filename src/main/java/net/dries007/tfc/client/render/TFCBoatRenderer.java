package net.dries007.tfc.client.render;

import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.util.ResourceLocation;

public class TFCBoatRenderer extends BoatRenderer
{
    private final ResourceLocation location;

    public TFCBoatRenderer(EntityRendererManager renderManagerIn, ResourceLocation location)
    {
        super(renderManagerIn);
        this.location = location;
    }

    @Override
    public ResourceLocation getTextureLocation(BoatEntity entityIn)
    {
        return location;
    }
}
