package net.dries007.tfc.util;

import java.util.HashMap;
import java.util.Map;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rock.Ore;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.world.feature.vein.VeinFeature;

public enum VeinAuditor
{
    INSTANCE;

    private boolean enabled = false;
    private int chunksGenerated = 0;
    private int veinsGenerated = 0;
    @Nullable private Rock lockRock;
    private final Object2IntMap<Ore> veins = new Object2IntOpenHashMap<>();

    public static boolean isEnabled()
    {
        return INSTANCE.enabled;
    }

    public static void setEnabled(boolean enabled)
    {
        INSTANCE.veins.clear();
        INSTANCE.enabled = enabled;
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<Block, Ore> ORE_MAP = Util.make(() -> {
        Map<Block, Ore> stateMap = new HashMap<>();
        TFCBlocks.GRADED_ORES.forEach((rock, map) -> map.forEach((ore, blocks) -> blocks.values().forEach(reg -> stateMap.put(reg.get(), ore))));
        TFCBlocks.ORES.forEach((rock, map) -> map.forEach((ore, reg) -> stateMap.put(reg.get(), ore)));
        return stateMap;
    });

    public static void markChunkGenerated()
    {
        if (isEnabled())
        {
            INSTANCE.chunksGenerated += 1;
        }
    }

    public static void markVeinGenerated(BlockState oreState)
    {
        if (isEnabled())
        {
            INSTANCE.veinsGenerated += 1;
            final Ore ore = ORE_MAP.get(oreState.getBlock());
            if (ore != null)
            {
                INSTANCE.veins.mergeInt(ore, 1, Integer::sum);
            }
        }
    }

    public static void print(ServerLevel server)
    {
        if (isEnabled())
        {
            final long veinFeatures = server.registryAccess().registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY).stream().filter(feature -> feature.feature() instanceof VeinFeature<?, ?>).count();

            LOGGER.info("Chunks marked as used by a vein feature: {}, given {} vein features, this means {} chunks were explored. A total of {} veins placed at least one block.", INSTANCE.chunksGenerated, veinFeatures, INSTANCE.chunksGenerated / veinFeatures, INSTANCE.veinsGenerated);
            LOGGER.info("=======================================================================================================");
            INSTANCE.veins.forEach((ore, amount) -> {
                LOGGER.info("Ore: {} Chunks Appeared In: {}", ore.name(), amount);
            });
        }
    }

}
