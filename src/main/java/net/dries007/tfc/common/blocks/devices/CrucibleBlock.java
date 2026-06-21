/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.devices;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;

import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.DirectionPropertyBlock;
import net.dries007.tfc.common.blocks.EntityBlockExtension;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TooltipBlock;
import net.dries007.tfc.common.component.TFCComponents;
import net.dries007.tfc.common.component.block.CrucibleComponent;
import net.dries007.tfc.common.component.size.IItemSize;
import net.dries007.tfc.common.component.size.Size;
import net.dries007.tfc.common.component.size.Weight;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.tooltip.Tooltips;

public class CrucibleBlock extends DeviceBlock implements EntityBlockExtension, IItemSize, TooltipBlock
{

    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    static
    {
        final VoxelShape base  = Shapes.or(
            box(3, 0, 3, 13, 2, 13), // base
            box(1, 1, 1, 15, 16, 3), // north
            box(1, 1, 13, 15, 16, 15), // south
            box(13, 1, 1, 15, 16, 15), // east
            box(1, 1, 1, 3, 16, 15) // west
        );

        final VoxelShape east  = box(15.0D, 0.0D, 5.0D,  16.0D, 5.0D, 11.0D);
        final VoxelShape south = box(5.0D,  0.0D, 15.0D, 11.0D, 5.0D, 16.0D);
        final VoxelShape west  = box(0.0D,  0.0D, 5.0D,  1.0D,  5.0D, 11.0D);
        final VoxelShape north = box(5.0D,  0.0D, 0.0D,  11.0D, 5.0D, 1.0D);

        final VoxelShape[] directions = new VoxelShape[] {south, west, north, east};

        for (int i = 0; i < SHAPES.length; i++)
        {
            VoxelShape shape = base;
            for (Direction direction : Direction.Plane.HORIZONTAL)
            {
                if (((i >> direction.get2DDataValue()) & 1) == 1)
                {
                    shape = Shapes.or(shape, directions[direction.get2DDataValue()]);
                }
            }
            SHAPES[i] = shape;
        }
    }

    /**
     * Full interaction shape when placing blocks against the composter, as otherwise targeting the top is quite difficult
     */
    private static final VoxelShape INTERACTION_SHAPE = Shapes.or(
        box(3, 0, 3, 13, 2, 13), // base
        box(1, 1, 1, 15, 16, 15) // interior
    );

    private static final BooleanProperty NORTH = PipeBlock.NORTH;
    private static final BooleanProperty EAST = PipeBlock.EAST;
    private static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    private static final BooleanProperty WEST = PipeBlock.WEST;

    public CrucibleBlock(ExtendedProperties properties)
    {
        super(properties, InventoryRemoveBehavior.SAVE);
        this.registerDefaultState(this.defaultBlockState().setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(NORTH, false));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        if (!player.isShiftKeyDown())
        {
            if (player instanceof ServerPlayer serverPlayer)
            {
                level.getBlockEntity(pos, TFCBlockEntities.CRUCIBLE.get()).ifPresent(crucible -> serverPlayer.openMenu(crucible, crucible.getBlockPos()));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES[(state.getValue(NORTH) ? 1 << Direction.NORTH.get2DDataValue() : 0) |
            (state.getValue(EAST) ? 1 << Direction.EAST.get2DDataValue() : 0) |
            (state.getValue(SOUTH) ? 1 << Direction.SOUTH.get2DDataValue() : 0) |
            (state.getValue(WEST) ? 1 << Direction.WEST.get2DDataValue() : 0)];
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos)
    {
        return INTERACTION_SHAPE;
    }

    @Override
    public Size getSize(ItemStack stack)
    {
        return Size.HUGE;
    }

    @Override
    public Weight getWeight(ItemStack stack)
    {
        return stack.getOrDefault(TFCComponents.CRUCIBLE, CrucibleComponent.EMPTY).isEmpty()
            ? Weight.HEAVY
            : Weight.VERY_HEAVY;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState adjacentState, LevelAccessor level, BlockPos pos, BlockPos adjacentPos)
    {
        for (final Direction planeDirection : Direction.Plane.HORIZONTAL)
        {
            final BlockPos thisAdjacentPos = pos.relative(planeDirection);
            final BlockState thisAdjacentState = level.getBlockState(thisAdjacentPos);
            final Block adjancentBlock = thisAdjacentState.getBlock();
            final boolean isAdjacentConnectable = adjancentBlock instanceof ChannelBlock;
            state = state.setValue(DirectionPropertyBlock.getProperty(planeDirection), isAdjacentConnectable);
        }

        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EAST, SOUTH, WEST, NORTH);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state)
    {
        return TFCConfig.SERVER.crucibleEnableAutomation.get();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos)
    {
        if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)
        {
            final FluidStack fluid = crucible.getInventory().getFluidInTank(0);
            if (!fluid.isEmpty())
            {
                return Mth.clamp(fluid.getAmount() * 15 / TFCConfig.SERVER.crucibleCapacity.get(), 1, 15);
            }
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag)
    {
        final CrucibleComponent component = stack.getOrDefault(TFCComponents.CRUCIBLE, CrucibleComponent.EMPTY);
        if (!component.isEmpty())
        {
            if (!TFCConfig.CLIENT.displayItemContentsAsImages.get())
            {
                tooltip.add(Component.translatable("tfc.tooltip.small_vessel.contents").withStyle(ChatFormatting.DARK_GREEN));
                Helpers.addInventoryTooltipInfo(component.itemContent(), tooltip);
            }

            final FluidStack fluid = component.fluidContent().getResult();
            if (!fluid.isEmpty())
            {
                tooltip.add(Tooltips.fluidUnitsOf(fluid));
            }
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack)
    {
        return TFCConfig.CLIENT.displayItemContentsAsImages.get()
            ? TooltipBlock.buildInventoryTooltip(
                stack.getOrDefault(TFCComponents.CRUCIBLE, CrucibleComponent.EMPTY)
                    .itemContent()
                    .subList(CrucibleBlockEntity.SLOT_INPUT_START, 1 + CrucibleBlockEntity.SLOT_INPUT_END),
                3, 3)
            : Optional.empty();
    }
}
