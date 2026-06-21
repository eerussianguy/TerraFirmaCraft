/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client.model.entity;

// Made with Blockbench 5.0.3
// Exported for Minecraft version 1.17 or later with Mojang mappings

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import net.dries007.tfc.common.entities.aquatic.FreshwaterFish;

public class PeacockBassModel extends HierarchicalAnimatedModel<FreshwaterFish>
{
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart tailfin;

    public PeacockBassModel(ModelPart root)
    {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftFin = this.body.getChild("leftFin");
        this.rightFin = this.body.getChild("rightFin");
        this.tailfin = this.body.getChild("tailfin");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(1, 1).addBox(-1.0F, -1.5F, -5.0F, 2.0F, 4.0F, 7.0F, new CubeDeformation(0.0F))
            .texOffs(16, 14).addBox(0.0F, -3.5F, -4.0F, 0.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 21.0F, 1.0F));

        PartDefinition piece_r1 = body.addOrReplaceChild("piece_r1", CubeListBuilder.create().texOffs(26, 26).addBox(0.0F, 0.5F, 0.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, -4.0F, 0.0F, 0.0F, 0.5236F));

        PartDefinition piece_r2 = body.addOrReplaceChild("piece_r2", CubeListBuilder.create().texOffs(26, 26).addBox(0.0F, 0.5F, 0.0F, 0.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, -4.0F, 0.0F, 0.0F, -0.5236F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(20, 0).addBox(-1.5F, -0.5F, -2.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -5.0F));

        PartDefinition leftFin = body.addOrReplaceChild("leftFin", CubeListBuilder.create().texOffs(1, 28).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 1.0F, -4.0F, 0.0F, 0.4363F, 0.0F));

        PartDefinition rightFin = body.addOrReplaceChild("rightFin", CubeListBuilder.create().texOffs(22, 18).addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 1.0F, -4.0F, 0.0F, -0.4363F, 0.0F));

        PartDefinition tailfin = body.addOrReplaceChild("tailfin", CubeListBuilder.create().texOffs(6, 15).addBox(0.0F, -3.0F, 1.0F, 0.0F, 7.0F, 5.0F, new CubeDeformation(0.0F))
            .texOffs(22, 9).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(FreshwaterFish entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)
    {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.animateWalk(SWIM, limbSwing, limbSwingAmount, 1f, 2.5f);
    }

    @Override
    public ModelPart root()
    {
        return this.root;
    }

    public static final AnimationDefinition SWIM = AnimationDefinition.Builder.withLength(0.7864F).looping()
        .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -17.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.0414F, KeyframeAnimations.degreeVec(0.0F, -19.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1794F, KeyframeAnimations.degreeVec(0.4885F, -5.5656F, -5.0237F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3587F, KeyframeAnimations.degreeVec(0.0F, 12.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4001F, KeyframeAnimations.degreeVec(0.0F, 14.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.6071F, KeyframeAnimations.degreeVec(-0.2305F, -2.633F, 5.0053F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7864F, KeyframeAnimations.degreeVec(0.0F, -17.5F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
            new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7864F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("tailfin", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.2069F, KeyframeAnimations.degreeVec(0.0F, -25.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.5381F, KeyframeAnimations.degreeVec(0.0F, 21.65F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7864F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("leftFin", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3725F, KeyframeAnimations.degreeVec(4.8669F, 28.6745F, 4.029F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7864F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .addAnimation("rightFin", new AnimationChannel(AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3725F, KeyframeAnimations.degreeVec(0.0F, -32.5F, 0.0F), AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.7864F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
        ))
        .build();
}