/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PufferfishRenderer;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.phys.Vec3;

public class TFCPufferfishRenderer extends PufferfishRenderer
{
    public TFCPufferfishRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    protected void setupRotations(Pufferfish entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale)
    {
        // handle patchouli
        final Vec3 pos = entity.position();
        if (Math.abs(pos.x) < 0.01f && Math.abs(pos.y) < 0.01f && Math.abs(pos.z) < 0.01f)
            return;
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
    }
}
