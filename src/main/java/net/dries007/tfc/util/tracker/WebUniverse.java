package net.dries007.tfc.util.tracker;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blockentities.RotatingBlockEntity;

public final class WebUniverse
{
    private static Map<LevelAccessor, Map<Long, MechanicalWeb>> WEBS = new HashMap<>();

    public static void onLevelLoaded(LevelAccessor level)
    {
        WEBS.put(level, new HashMap<>());
    }

    public static void onLevelUnloaded(LevelAccessor level)
    {
        WEBS.remove(level);
    }

    public static void remove(RotatingBlockEntity blockEntity)
    {
        final Level level = blockEntity.getLevel();
        if (level != null)
        {
            WEBS.get(level).remove(blockEntity.getWebId());
        }
    }

    @Nullable
    public static MechanicalWeb getOrCreateWeb(RotatingBlockEntity blockEntity)
    {
        Long id = blockEntity.getWebId();
        MechanicalWeb web;
        if (blockEntity.getLevel() == null)
        {
            return null;
        }
        final var map = WEBS.computeIfAbsent(blockEntity.getLevel(), l -> new HashMap<>());
        if (id == null)
        {
            return null;
        }
        if (!map.containsKey(id))
        {
            web = new MechanicalWeb();
            web.id = blockEntity.getWebId();
            map.put(id, web);
        }
        web = map.get(id);
        return web;
    }
}
