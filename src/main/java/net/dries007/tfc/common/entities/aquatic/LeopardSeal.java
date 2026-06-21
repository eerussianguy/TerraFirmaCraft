/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities.aquatic;

import com.mojang.serialization.Dynamic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.ai.amphibian.PinnipedAI;
import net.dries007.tfc.util.Helpers;

public class LeopardSeal extends AmphibiousAnimal
{

    public static AttributeSupplier.Builder createAttributes()
    {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0D).add(Attributes.MOVEMENT_SPEED, 1.0D).add(Attributes.ATTACK_DAMAGE, 6.0D).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public LeopardSeal(EntityType<? extends AmphibiousAnimal> type, Level level)
    {
        super(type, level, TFCSounds.SEAL);
    }

    // TODO: Would like leopard seals to defend themselves rather than play dead
    @Override
    public boolean isPlayingDeadEffective()
    {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack)
    {
        return Helpers.isItem(stack, TFCTags.Items.SEAL_FOOD);
    }

    @Override
    protected SoundEvent getAmbientSound()
    {
        return ambient.get();
    }

    public void playAmbientSound()
    {
        if (!this.isInWaterOrBubble())
        {
            super.playAmbientSound();
        }
    }

    @Override
    protected Brain.Provider<? extends AmphibiousAnimal> brainProvider()
    {
        return Brain.provider(PinnipedAI.MEMORY_TYPES, PinnipedAI.SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic)
    {
        return PinnipedAI.makeBrain(brainProvider().makeBrain(dynamic));
    }

    @Override
    protected void customServerAiStep()
    {
        getBrain().tick((ServerLevel) level(), this);
        PinnipedAI.updateActivity(this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        return super.hurt(source, amount);
    }
}
