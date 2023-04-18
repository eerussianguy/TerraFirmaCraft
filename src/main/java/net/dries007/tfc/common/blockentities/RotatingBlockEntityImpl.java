package net.dries007.tfc.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.util.tracker.MechanicalWeb;
import net.dries007.tfc.util.tracker.WebUniverse;

public class RotatingBlockEntityImpl extends TFCBlockEntity implements RotatingBlockEntity
{
    public static void serverTick(Level level, BlockPos pos, BlockState state, RotatingBlockEntityImpl rotating)
    {
        rotating.checkForLastTickSync();
        if (rotating.speedDirty)
        {
            rotating.attach();
            rotating.speedDirty = false;
        }
        if (rotating.networkDirty)
        {
            if (rotating.validationCountdown-- <= 0)
            {
                rotating.validationCountdown = 40;
                rotating.validate();
            }
            if (rotating.hasWeb())
            {
                final MechanicalWeb web = WebUniverse.getOrCreateWeb(rotating);
                if (web != null)
                {
                    web.syncAllMembers();
                }
            }
        }
    }

    protected int speed;
    protected boolean needsClientUpdate;
    protected boolean isDirty;
    protected boolean speedDirty = true;

    @Nullable private Long webId;
    @Nullable private BlockPos source;
    private boolean networkDirty;
    private boolean updateSpeed;
    private int networkSize;
    private int validationCountdown = 20;

    public RotatingBlockEntityImpl(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    public void checkForLastTickSync()
    {
        if (needsClientUpdate)
        {
            // only sync further down when we actually request it to be synced
            needsClientUpdate = false;
            super.markForSync();
        }
        if (isDirty)
        {
            isDirty = false;
            super.markDirty();
        }
    }

    @Override
    public void markForSync()
    {
        needsClientUpdate = true;
    }

    @Override
    public void markDirty()
    {
        isDirty = true;
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        onAddedToLevel();
    }


    @Override
    protected void loadAdditional(CompoundTag tag)
    {
        super.loadAdditional(tag);
        loadRotatingData(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        saveRotatingData(tag);
    }

    @Override
    public void updateFromNetwork(int size)
    {
        this.networkSize = size;
        markForSync();
    }

    @Override
    public void setSource(BlockPos pos)
    {
        this.source = pos;
        markForSync();
    }

    @Nullable
    @Override
    public BlockPos getSource()
    {
        return source;
    }

    @Override
    public void setSpeed(int speed)
    {
        this.speed = speed;
        speedDirty = true;
        markForSync();
    }

    @Override
    public int getSpeed()
    {
        return speed;
    }

    @Override
    public void removeSource()
    {
        speed = 0;
        source = null;
        webId = null;
        markForSync();
    }

    @Override
    public void markNetworkDirty()
    {
        networkDirty = true;
        markForSync();
    }


    @Nullable
    public Long getWebId()
    {
        return webId;
    }

    @Override
    public void setWebIdUnchecked(@Nullable Long id)
    {
        this.webId = id;
    }

    public void setWebId(@Nullable Long id)
    {
        if (webId != null && webId.equals(id) || (id == null && webId == null))
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
        webId = id;
        markForSync();
        if (webId != null)
        {
            final var web = WebUniverse.getOrCreateWeb(this);
            if (web != null)
            {
                web.initialized = true;
                web.addAndUpdate(this);
            }
        }
    }

    @Override
    public int getNetworkSize()
    {
        return networkSize;
    }

    @Override
    public void setNetworkSize(int size)
    {
        this.networkSize = size;
    }
}
