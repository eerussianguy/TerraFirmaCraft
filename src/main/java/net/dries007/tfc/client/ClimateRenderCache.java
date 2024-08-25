/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.tracker.WeatherHelpers;

/**
 * This stores the climate parameters at the current client player location, for quick lookup in rendering purposes
 */
public enum ClimateRenderCache
{
    INSTANCE;

    private float averageTemperature;
    private float temperature;
    private float averageRainfall;
    private float rainVariance;
    private float rainfall;
    private float baseGroundwater;
    private float averageGroundwater;
    private float groundwater;
    private Vec2 wind = Vec2.ZERO;

    private float lastRainLevel, currRainLevel;

    /**
     * Called on client tick, updates the client parameters for the current client player location
     */
    public void onClientTick()
    {
        final Level level = ClientHelpers.getLevel();
        final Player player = ClientHelpers.getPlayer();
        if (level != null && player != null)
        {
            final BlockPos pos = player.blockPosition();
            final ClimateModel model = Climate.get(level);

            averageTemperature = model.getAverageTemperature(level, pos);
            temperature = model.getTemperature(level, pos);
            averageRainfall = model.getAverageRainfall(level, pos);
            rainVariance = model.getRainfallVariance(level, pos);
            rainfall = model.getRainfall(level, pos);
            baseGroundwater = model.getBaseGroundwater(level, pos);
            averageGroundwater = model.getAverageGroundwater(level, pos);
            groundwater = model.getGroundwater(level, pos);
            wind = model.getWind(level, pos);

            // Calculate a real rain level to interpolate from on client. This reads the level's rain level, which includes influence
            // from climate, but doesn't include local influences.
            if (model.supportsRain())
            {
                final boolean isRaining = WeatherHelpers.isPrecipitating(
                    model.getRain(Calendars.CLIENT.getCalendarTicks()),
                    rainfall
                );

                lastRainLevel = currRainLevel;
                currRainLevel = Mth.clamp(currRainLevel + (isRaining ? 0.01f : -0.01f), 0, 1);
            }
            else
            {
                // Default vanilla behavior, just redirected
                lastRainLevel = level.oRainLevel;
                currRainLevel = level.rainLevel;
            }
        }
    }

    public float getAverageTemperature()
    {
        return averageTemperature;
    }

    public float getTemperature()
    {
        return temperature;
    }

    public float getRainfall()
    {
        return rainfall;
    }

    public float getAverageRainfall()
    {
        return averageRainfall;
    }

    public float getRainVariance()
    {
        return rainVariance;
    }

    public float getRainLevel(float partialTick)
    {
        return Mth.lerp(partialTick, lastRainLevel, currRainLevel);
    }

    public float getBaseGroundwater()
    {
        return baseGroundwater;
    }

    public float getAverageGroundwater()
    {
        return averageGroundwater;
    }

    public float getGroundwater()
    {
        return groundwater;
    }

    public Vec2 getWind()
    {
        return wind;
    }
}
