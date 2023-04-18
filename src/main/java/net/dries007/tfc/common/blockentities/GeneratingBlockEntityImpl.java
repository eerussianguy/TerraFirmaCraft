package net.dries007.tfc.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GeneratingBlockEntityImpl extends RotatingBlockEntityImpl
{
    public static void serverTickGenerating(Level level, BlockPos pos, BlockState state, GeneratingBlockEntityImpl rotating)
    {
        RotatingBlockEntityImpl.serverTick(level, pos, state, rotating);
        if (rotating.sourceNeedsUpdating)
        {
            rotating.sourceNeedsUpdating = false;
            rotating.updateGeneration();
        }
    }

    public boolean sourceNeedsUpdating;

    public GeneratingBlockEntityImpl(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    public void updateGeneration()
    {
        final int speed = getGeneratedSpeed();
        final int prevSpeed = this.speed;

        if (level == null || level.isClientSide) return;

        if (speed != prevSpeed)
        {
            this.speed = speed;
            reactToSpeed(prevSpeed, speed);
        }
    }


    public void reactToSpeed(int oldSpeed, int newSpeed)
    {
        if (newSpeed == 0)
        {
            setSpeed(0);
            detach();
            setWebId(null);
        }
        else if (oldSpeed == 0)
        {
            setSpeed(newSpeed);
            setWebId(createWebId());
            attach();
        }
        else if (hasSource())
        {
            if (Math.abs(newSpeed) > Math.abs(oldSpeed))
            {
                detach();
                setSpeed(newSpeed);
                removeSource();
                setWebId(createWebId());
                attach();
            }
        }
        else
        {
            detach();
            setSpeed(newSpeed);
            attach();
        }
    }

    @Override
    public void removeSource()
    {
        if (hasSource() && isSource())
        {
            sourceNeedsUpdating = true;
        }
        super.removeSource();
    }

    @Override
    public void setSource(BlockPos pos)
    {
        super.setSource(pos);
        assert level != null;
        if (level.getBlockEntity(pos) instanceof RotatingBlockEntity rotating)
        {
            if (sourceNeedsUpdating && Math.abs(rotating.getSpeed()) >= Math.abs(getGeneratedSpeed()))
            {
                sourceNeedsUpdating = false;
            }
        }

    }

    public long createWebId()
    {
        return worldPosition.asLong();
    }
}
