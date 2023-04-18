package net.dries007.tfc.common.blockentities;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.Rotator;
import net.dries007.tfc.util.tracker.MechanicalWeb;
import net.dries007.tfc.util.tracker.WebHelpers;
import net.dries007.tfc.util.tracker.WebUniverse;

public interface RotatingBlockEntity
{
    default BlockEntity getBlockEntity()
    {
        return (BlockEntity) this;
    }

    @Nullable
    default Level getLevel()
    {
        return getBlockEntity().getLevel();
    }

    default void onAddedToLevel()
    {
        final Level level = getBlockEntity().getLevel();
        assert level != null;
        if (hasWeb() && !level.isClientSide)
        {
            final MechanicalWeb web = WebUniverse.getOrCreateWeb(this);
            if (web != null)
            {
                if (!web.initialized)
                {
                    web.initialize(getNetworkSize());
                }
                web.addWithoutUpdating(this);
            }
        }
    }

    default void validate()
    {
        final Level level = getBlockEntity().getLevel();
        assert level != null;
        if (hasSource())
        {
            if (!hasWeb())
            {
                removeSource();
                return;
            }
            if (getSource() == null || !level.isLoaded(getSource()))
            {
                return;
            }
            if (!(level.getBlockEntity(getSource()) instanceof RotatingBlockEntity))
            {
                removeSource();
                detach();
            }
        }
    }

    default void detach()
    {
        final Level level = getBlockEntity().getLevel();
        assert level != null;
        WebHelpers.removeConnection(level, getBlockEntity().getBlockPos(), this);
    }

    default void attach()
    {
        final Level level = getBlockEntity().getLevel();
        assert level != null;
        WebHelpers.rotatorAdded(level, getBlockEntity().getBlockPos(), this);
    }

    default void onRemoved()
    {
        final Level level = getBlockEntity().getLevel();
        assert level != null;
        if (!level.isClientSide)
        {
            if (hasWeb())
            {
                final MechanicalWeb web = WebUniverse.getOrCreateWeb(this);
                if (web != null)
                {
                    web.remove(this);
                }
                detach();
            }
        }
    }

    default void loadRotatingData(CompoundTag tag)
    {
        if (tag.contains("source", Tag.TAG_COMPOUND))
        {
            setSource(NbtUtils.readBlockPos(tag.getCompound("source")));
        }
        if (tag.contains("webId", Tag.TAG_LONG))
        {
            setWebIdUnchecked(tag.getLong("webId"));
            setNetworkSize(tag.getInt("networkSize"));
        }
        setSpeed(tag.getInt("speed"));
    }

    default void saveRotatingData(CompoundTag tag)
    {
        if (getSource() != null)
        {
            tag.put("source", NbtUtils.writeBlockPos(getSource()));
        }
        if (getWebId() != null)
        {
            tag.putLong("webId", getWebId());
            tag.putInt("networkSize", getNetworkSize());
        }
        tag.putInt("speed", getSpeed());
    }

    void updateFromNetwork(int size);

    default int getGeneratedSpeed()
    {
        return 0;
    }

    default List<BlockPos> addConnectedPositions(Rotator block, BlockState state, List<BlockPos> neighbors)
    {
        return neighbors;
    }

    default boolean isSource()
    {
        return getGeneratedSpeed() != 0;
    }

    default boolean hasSource()
    {
        return getSource() != null;
    }

    void setSource(BlockPos pos);

    @Nullable
    BlockPos getSource();

    void setSpeed(int speed);

    int getSpeed();

    void removeSource();

    void markNetworkDirty();

    @Nullable
    Long getWebId();

    void setWebIdUnchecked(@Nullable Long id);

    default void setWebId(@Nullable Long id)
    {
        if (getWebId() != null && getWebId().equals(id) || (id == null && getWebId() == null))
        {
            return;
        }
        if (id != null)
        {
            final var web = WebUniverse.getOrCreateWeb(this);
            if (web != null)
            {
                web.remove(this);
            }
        }
        setWebIdUnchecked(id);
        if (getWebId() != null)
        {
            final var web = WebUniverse.getOrCreateWeb(this);
            if (web != null)
            {
                web.initialized = true;
                web.addAndUpdate(this);
            }
        }
    }

    default boolean hasWeb()
    {
        return getWebId() != null;
    }

    @Nullable
    default MechanicalWeb getWeb()
    {
        return WebUniverse.getOrCreateWeb(this);
    }

    int getNetworkSize();

    void setNetworkSize(int size);
}
