package net.dries007.tfc.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.client.particle.TFCParticles;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.devices.DeviceBlock;
import net.dries007.tfc.common.capabilities.BlockCapabilities;
import net.dries007.tfc.common.component.glass.GlassOperation;
import net.dries007.tfc.common.component.glass.GlassWorking;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.FluidProperty;
import net.dries007.tfc.common.fluids.IFluidLoggable;
import net.dries007.tfc.util.Helpers;

public class HatchBlock extends DeviceBlock implements IFluidLoggable
{
    public static ItemStack dispense(Level level, BlockPos pos, boolean drop)
    {
        final BlockEntity be = level.getBlockEntity(pos.above());
        if (be == null)
            return ItemStack.EMPTY;
        final IItemHandler inv = Helpers.getCapability(BlockCapabilities.ITEM, be, Direction.DOWN);
        if (inv == null)
            return ItemStack.EMPTY;

        for (int i = 0; i < inv.getSlots(); i++)
        {
            final ItemStack item = inv.getStackInSlot(i);
            if (!item.isEmpty())
            {
                final ItemStack extracted = inv.extractItem(i, 1, false);
                if (drop)
                    Helpers.spawnItem(level, pos, extracted, 0.5, 0, 0, 0);
                return extracted;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean interactWithBlowpipe(Level level, BlockPos pos, ItemStack blowpipe)
    {
        final BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(POWERED) || state.getValue(POWERED))
            return false;
        level.setBlockAndUpdate(pos, state.setValue(POWERED, true));
        level.scheduleTick(pos, state.getBlock(), 80);

        final ItemStack dispensed = dispense(level, pos, false);
        final GlassOperation op = GlassOperation.getByPowder(dispensed);
        if (op == null)
        {
            if (Helpers.isItem(dispensed, TFCTags.Items.GLASS_BATCHES) && Helpers.isItem(blowpipe, TFCTags.Items.TOOLS_BLOWPIPE))
            {
                GlassWorking.createNewBatch(blowpipe, dispensed);
                return true;
            }
            Helpers.spawnItem(level, pos, dispensed, 0.5, 0, 0, 0);
            return true;
        }
        GlassWorking.apply(blowpipe, op);
        if (level instanceof ServerLevel server)
        {
            for (int i = 0; i < 10; i++)
            {
                server.sendParticles(new ItemParticleOption(TFCParticles.DUST.get(), dispensed), pos.getX() + level.random.nextFloat(), pos.getY() + 0.75, pos.getZ() + level.random.nextFloat(), 1, 0, 0, 0, 0.01);
            }
        }
        return true;
    }

    public static final VoxelShape SHAPE = box(0, 14, 0, 16, 16, 16);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final FluidProperty FLUID = TFCBlockStateProperties.ALL_WATER;

    public HatchBlock(ExtendedProperties properties)
    {
        super(properties, InventoryRemoveBehavior.NOOP);
        registerDefaultState(getStateDefinition().any().setValue(POWERED, false));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (!level.hasNeighborSignal(pos))
        {
            level.setBlockAndUpdate(pos, state.setValue(POWERED, false));
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState state = super.getStateForPlacement(context);
        final FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        if (state != null)
        {
            if (!fluidState.isEmpty())
                state = state.setValue(getFluidProperty(), getFluidProperty().keyForOrEmpty(fluidState.getType()));
            state = state.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
        }
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving)
    {
        final boolean signal = level.hasNeighborSignal(pos);
        if (signal != state.getValue(POWERED))
        {
            level.setBlockAndUpdate(pos, state.setValue(POWERED, signal));
            Helpers.playSound(level, pos, signal ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE);
            if (signal)
                dispense(level, pos, true);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos)
    {
        FluidHelpers.tickFluid(level, currentPos, state);
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return IFluidLoggable.super.getFluidState(state);
    }

    @Override
    public FluidProperty getFluidProperty()
    {
        return FLUID;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder.add(POWERED, FLUID));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }
}
