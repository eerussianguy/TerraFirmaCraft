/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.items;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.crop.Crop;
import net.dries007.tfc.util.climate.ClimateRange;

public class SeedItem extends ItemNameBlockItem implements PlantableInfo
{
    protected final Block deadBlock;
    private final PlantNutrients nutrients;
    private final Supplier<ClimateRange> climateRange;

    public SeedItem(Crop crop, Block block, Block deadBlock, Properties properties)
    {
        super(block, properties);
        this.deadBlock = deadBlock;
        nutrients = new PlantNutrients(crop.getNitrogen(), crop.getPhosphorous(), crop.getPotassium());
        climateRange = crop.getClimateRange();
    }

    @Override
    public @Nullable PlantableInfo.PlantNutrients getNutrientsInfo()
    {
        return nutrients;
    }

    @Override
    public @Nullable ClimateRange getClimateRangeInfo()
    {
        return climateRange.get();
    }

    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item)
    {
        super.registerBlocks(blockToItemMap, item);
        blockToItemMap.put(this.deadBlock, item);
    }
}
