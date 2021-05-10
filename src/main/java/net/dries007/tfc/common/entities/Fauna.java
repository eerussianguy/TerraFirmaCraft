package net.dries007.tfc.common.entities;

import com.google.gson.JsonObject;
import net.minecraft.entity.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import com.mojang.serialization.JsonOps;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.decorator.ClimateConfig;

public class Fauna
{
    private final ResourceLocation id;
    private final EntityType<MobEntity> entity;
    private final Fluid fluid;
    private final int chance;
    private final int distanceBelowSeaLevel;
    private final ClimateConfig climateConfig;

    public Fauna(ResourceLocation id, JsonObject json)
    {
        this.id = id;
        //noinspection unchecked
        entity = (EntityType<MobEntity>) ForgeRegistries.ENTITIES.getValue(new ResourceLocation(JSONUtils.getAsString(json, "entity")));
        this.fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(JSONUtils.getAsString(json, "fluid")));
        this.chance = JSONUtils.getAsInt(json, "chance");
        this.distanceBelowSeaLevel = JSONUtils.getAsInt(json, "distance_below_sea_level");
        this.climateConfig = ClimateConfig.CODEC.decode(JsonOps.INSTANCE, json.get("climate")).getOrThrow(false, null).getFirst();
    }

    public ResourceLocation getId()
    {
        return id;
    }

    public EntityType<MobEntity> getEntity()
    {
        return entity;
    }

    public EntitySpawnPlacementRegistry.PlacementType getPlacementType()
    {
        if (fluid != null && fluid != Fluids.EMPTY)//todo: serialize this...
        {
            return EntitySpawnPlacementRegistry.PlacementType.IN_WATER;
        }
        return EntitySpawnPlacementRegistry.PlacementType.ON_GROUND;
    }

    public EntitySpawnPlacementRegistry.IPlacementPredicate<MobEntity> makeRules()
    {
        return (entity, world, reason, pos, rand) -> {
            if (rand.nextInt(chance) != 0) return false;

            if (fluid != null && fluid != Fluids.EMPTY)
            {
                Fluid foundFluid = world.getFluidState(pos).getType();
                if (foundFluid != fluid)
                {
                    return false;
                }
            }

            final int seaLevel = world.getLevel().getChunkSource().generator.getSeaLevel();
            if (distanceBelowSeaLevel != -1 && pos.getY() > seaLevel - distanceBelowSeaLevel)
            {
                return false;
            }

            if (climateConfig != null)
            {
                final ChunkDataProvider provider = ChunkDataProvider.getOrThrow(world);
                final ChunkData data = provider.get(pos, ChunkData.Status.CLIMATE);
                if (!climateConfig.isValid(data, pos, rand))
                {
                    return false;
                }
            }
            return true;
        };
    }

}