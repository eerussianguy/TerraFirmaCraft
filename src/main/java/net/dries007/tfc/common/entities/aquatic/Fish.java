/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities.aquatic;

import java.util.Locale;
import net.minecraft.sounds.SoundEvents;

import net.dries007.tfc.client.TFCSounds;

public enum Fish
{
    BLUEGILL,
    CRAPPIE,
    LAKE_TROUT,
    LARGEMOUTH_BASS,
    RAINBOW_TROUT,
    SALMON,
    SMALLMOUTH_BASS,
    NORTHERN_PIKE,
    BURBOT,
    ARCTIC_CHAR,
    MUKSUN,
    TILAPIA,
    SPOTTED_GUDGEON,
    PEACOCK_BASS,
    PACU,
    RED_PIRANHA;

    private final String serializedName;

    Fish()
    {
        serializedName = name().toLowerCase(Locale.ROOT);
    }

    public String getSerializedName()
    {
        return serializedName;
    }

    public TFCSounds.FishId makeSound()
    {
        if (this == SALMON)
        {
            return new TFCSounds.FishId(() -> SoundEvents.SALMON_AMBIENT, () -> SoundEvents.SALMON_DEATH, () -> SoundEvents.SALMON_HURT, () -> SoundEvents.SALMON_FLOP);
        }
        return TFCSounds.registerFish(serializedName);
    }

    public float getWidth()
    {
        return this == BLUEGILL ? 0.5f : 0.7f;
    }

    public float getHeight()
    {
        return this == BLUEGILL ? 0.3f : 0.4f;
    }

}
