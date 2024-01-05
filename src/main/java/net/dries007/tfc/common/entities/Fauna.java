/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import net.dries007.tfc.util.DataManager;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.JsonHelpers;
import net.dries007.tfc.world.chunkdata.ForestType;
import net.dries007.tfc.world.placement.ClimatePlacement;

/**
 * A data driven way to make spawning conditions for animals player configurable.
 */
public class Fauna
{
    public static final DataManager<Fauna> MANAGER = new DataManager<>(Helpers.identifier("fauna"), "fauna", Fauna::new);
    public static final Map<EntityType<?>, Fauna> PREDICATES = new HashMap<>();

    private static final ClimatePlacement DEFAULT_CLIMATE = new ClimatePlacement(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, ForestType.NONE, ForestType.OLD_GROWTH, false);

    private final ResourceLocation id;
    private final int chance;
    private final int distanceBelowSeaLevel;
    private final ClimatePlacement climate;
    private final boolean solidGround;
    private final int maxBrightness;

    public Fauna(ResourceLocation id, JsonObject json)
    {
        this.id = id;
        this.chance = JsonHelpers.getAsInt(json, "chance", 1);
        this.distanceBelowSeaLevel = JsonHelpers.getAsInt(json, "distance_below_sea_level", -1);
        this.climate = JsonHelpers.decodeCodecDefaulting(json, ClimatePlacement.PLACEMENT_CODEC, "climate", DEFAULT_CLIMATE);
        this.solidGround = JsonHelpers.getAsBoolean(json, "solid_ground", false);
        this.maxBrightness = JsonHelpers.getAsInt(json, "max_brightness", -1);
    }

    public ResourceLocation getId()
    {
        return id;
    }

    public int getChance()
    {
        return chance;
    }

    public int getDistanceBelowSeaLevel()
    {
        return distanceBelowSeaLevel;
    }

    public ClimatePlacement getClimate()
    {
        return climate;
    }

    public boolean isSolidGround()
    {
        return solidGround;
    }

    public int getMaxBrightness()
    {
        return maxBrightness;
    }
}
