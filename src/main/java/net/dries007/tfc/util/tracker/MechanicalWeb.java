package net.dries007.tfc.util.tracker;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blockentities.RotatingBlockEntity;

public class MechanicalWeb
{
    @Nullable
    public Long id = null;
    public boolean initialized;
    public List<RotatingBlockEntity> sources;
    public List<RotatingBlockEntity> members;

    private int unloadedMembers;

    public MechanicalWeb()
    {
        sources = new ArrayList<>();
        members = new ArrayList<>();
    }

    public void initialize(int members)
    {
        this.unloadedMembers = members;
        initialized = true;
    }

    public void addAndUpdate(RotatingBlockEntity blockEntity)
    {
        if (members.contains(blockEntity))
        {
            return;
        }
        if (blockEntity.isSource())
        {
            sources.add(blockEntity);
        }
        members.add(blockEntity);
        blockEntity.markNetworkDirty();
        blockEntity.updateFromNetwork(getSize());
    }

    public void addWithoutUpdating(RotatingBlockEntity blockEntity)
    {
        if (members.contains(blockEntity))
        {
            return;
        }
        if (blockEntity.isSource())
        {
            sources.add(blockEntity);
        }
        members.add(blockEntity);
        unloadedMembers = Math.max(unloadedMembers - 1, 0);
    }

    public void remove(RotatingBlockEntity blockEntity)
    {
        if (!members.contains(blockEntity))
        {
            return;
        }
        if (blockEntity.isSource())
        {
            sources.remove(blockEntity);
        }
        members.remove(blockEntity);
        blockEntity.updateFromNetwork(0);

        if (members.isEmpty())
        {
            WebUniverse.remove(blockEntity);
            return;
        }
        members.get(0).markNetworkDirty();
    }

    public void syncAllMembers()
    {
        members.forEach(mem -> mem.updateFromNetwork(0));
    }

    public int getSize()
    {
        return unloadedMembers + members.size();
    }
}
