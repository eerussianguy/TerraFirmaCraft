package net.dries007.tfc.common.blockentities.rotation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.dries007.tfc.common.blocks.rotation.AxleBlock;
import net.dries007.tfc.common.blocks.rotation.BeltSectionBlock;
import net.dries007.tfc.util.rotation.AxleNode;
import net.dries007.tfc.util.rotation.NetworkAction;
import net.dries007.tfc.util.rotation.Node;
import net.dries007.tfc.util.rotation.Rotation;

public class BeltSectionBlockEntity extends TFCBlockEntity implements RotatingBlockEntity
{
    public static void serverTick(Level level, BlockPos pos, BlockState state, BeltSectionBlockEntity belt)
    {

    }

    private boolean invalid;
    private final Node node;

    public BeltSectionBlockEntity(BlockPos pos, BlockState state)
    {
        this(TFCBlockEntities.BELT_SECTION.get(), pos, state);
    }

    public BeltSectionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);

        final Direction.Axis axis = state.getValue(BeltSectionBlock.AXIS);

        // todo: make these only connect to other belts.
        this.invalid = false;
        this.node = new AxleNode(pos, Node.ofAxis(axis)) {

            @Override
            protected void onInvalidConnection()
            {
                BeltSectionBlockEntity.this.onInvalidConnection();
            }

            @Override
            public String toString()
            {
                return "Belt[pos=%s, axis=%s]".formatted(axis, pos());
            }
        };
    }
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.putBoolean("invalid", invalid);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        invalid = tag.getBoolean("invalid");
    }

    @Override
    protected void onLoadAdditional()
    {
        performNetworkAction(NetworkAction.ADD);
    }

    @Override
    protected void onUnloadAdditional()
    {
        performNetworkAction(NetworkAction.REMOVE);
    }

    @Override
    public void markAsInvalidInNetwork()
    {
        invalid = true;
    }

    @Override
    public boolean isInvalidInNetwork()
    {
        return invalid;
    }

    @Override
    public Node getRotationNode()
    {
        return node;
    }

    private void onInvalidConnection()
    {
        if (level != null)
        {
            markAsInvalidInNetwork();
            level.scheduleTick(getBlockPos(), getBlockState().getBlock(), DELAY_FOR_INVALID_IN_NETWORK);
        }
    }

}
