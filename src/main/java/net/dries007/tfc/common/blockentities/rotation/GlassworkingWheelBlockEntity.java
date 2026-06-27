package net.dries007.tfc.common.blockentities.rotation;

import java.util.Arrays;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Vector3f;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.TickableInventoryBlockEntity;
import net.dries007.tfc.common.blocks.IGlassworkingDevice;
import net.dries007.tfc.common.component.glass.GlassWorking;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.items.BlowpipeItem;
import net.dries007.tfc.common.recipes.GlassworkingRecipe;
import net.dries007.tfc.common.component.heat.IHeat;
import net.dries007.tfc.common.component.heat.IHeatConsumer;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.rotation.NetworkAction;
import net.dries007.tfc.util.rotation.Node;
import net.dries007.tfc.util.rotation.Rotation;
import net.dries007.tfc.util.rotation.SinkNode;

public class GlassworkingWheelBlockEntity extends TickableInventoryBlockEntity<ItemStackHandler> implements RotationSinkBlockEntity, IHeatConsumer
{
    public static void serverTick(Level level, BlockPos pos, BlockState state, GlassworkingWheelBlockEntity wheel)
    {
        wheel.checkForLastTickSync();
        if (level.getGameTime() % 5 == 0 && !wheel.isInvalidInNetwork())
        {
            for (int slot = 0; slot < SLOTS; slot++)
            {
                final ItemStack stack = wheel.inventory.getStackInSlot(slot);
                if (stack.isEmpty())
                {
                    wheel.lastGlassStep[slot] = Integer.MIN_VALUE;
                    continue;
                }

                // Drive both selection and timing from one quantity: the world direction this blowpipe's glass (working) end
                // points, taken from the exact render angle the renderer draws it at. So the operated/animated blowpipe is
                // always the one drawn over the device, and the trigger fires exactly as its glass sweeps over it.
                final float glassAngle = wheel.getSlotRenderAngle(slot, 0f) + (float) 0;
                final Direction glassDir = directionFromAngle(glassAngle);
                final BlockPos relPos = pos.relative(glassDir);
                final BlockState relState = level.getBlockState(relPos);

                // A solid, non-device block in the glass's path knocks the blowpipe loose.
                if (!relState.isAir() && !Helpers.isBlock(relState, TFCTags.Blocks.GLASSWORKING_WHEEL_INVISIBLE))
                {
                    Helpers.spawnItem(level, relPos, wheel.inventory.extractItem(slot, 1, false), 0.5f, 0, 0, 0);
                    wheel.markForSync();
                    wheel.lastGlassStep[slot] = Integer.MIN_VALUE;
                    continue;
                }

                // The apex is where the glass end is drawn directly over a cardinal - i.e. when glassAngle crosses a multiple
                // of HALF_PI, which is where floor() ticks over. The wheel moves well under a quarter turn per sample, so a real
                // crossing shifts the step by exactly one; the 2*PI wrap of getRotationAngle instead shifts it by four (the angle
                // is clamped to [0, 2*PI)). Fire on an odd step delta (mod 4) to catch true crossings and ignore the wrap, and
                // prime on first sight so a freshly inserted blowpipe waits for its next real pass rather than firing immediately.
                final int glassStep = Mth.floor(glassAngle / Mth.HALF_PI);
                final int prevStep = wheel.lastGlassStep[slot];
                wheel.lastGlassStep[slot] = glassStep;
                final boolean apex = prevStep != Integer.MIN_VALUE && Math.floorMod(glassStep - prevStep, 4) % 2 != 0;

                if (apex && relState.getBlock() instanceof IGlassworkingDevice device)
                {
                    final int stepsBefore = GlassWorking.get(stack).steps().size();
                    if (device.interactWithBlowpipe(level, relPos, stack))
                    {
                        // Only attempt recipe completion when an operation step was actually added to the glass (e.g. not when
                        // a hatch merely ejects a non-powder item).
                        if (GlassWorking.get(stack).steps().size() != stepsBefore)
                        {
                            // If the blowpipe now matches a recipe, complete it: eject the result and reset the blowpipe to
                            // its empty form, so the wheel can keep cycling it (e.g. re-batched by a hatch) for the next run.
                            final GlassworkingRecipe recipe = GlassworkingRecipe.get(level, stack);
                            if (recipe != null)
                            {
                                Helpers.spawnItem(level, relPos, recipe.getResultItem(level.registryAccess()), 0.5f, 0, 0, 0);
                                wheel.inventory.setStackInSlot(slot, BlowpipeItem.transform(stack.getItem()).getDefaultInstance());
                            }
                        }
                        wheel.markForSync();
                    }
                }
            }
        }
    }

    /**
     * @return The horizontal {@link Direction} that the angle {@code radians} points, in the wheel renderer's frame: a {@code +Z}
     * vector rotated by {@code radians} about {@code Y} lands on this cardinal. Matches {@link #getSlotForDirection}'s convention.
     */
    private static Direction directionFromAngle(float radians)
    {
        return switch (Math.floorMod(Math.round(radians / Mth.HALF_PI), 4))
        {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.NORTH;
            default -> Direction.WEST;
        };
    }

    public static final int SLOTS = 4;

    private final Node node;

    /** Per-slot floor(glassAngle / HALF_PI) from the last tick; an edge marks the glass end sweeping over a new cardinal (a pass). */
    private final int[] lastGlassStep = new int[SLOTS];

    public GlassworkingWheelBlockEntity(BlockPos pos, BlockState state)
    {
        super(TFCBlockEntities.GLASSWORKING_WHEEL.get(), pos, state, defaultInventory(SLOTS));
        Arrays.fill(lastGlassStep, Integer.MIN_VALUE);
        this.node = new SinkNode(pos, Direction.UP) {
            @Override
            public String toString()
            {
                return "GlassworkingWheel[pos=%s]".formatted(pos());
            }
        };
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return Helpers.isItem(stack, TFCTags.Items.BLOWPIPES);
    }

    @Override
    public int getSlotStackLimit(int slot)
    {
        return 1;
    }

    @Override
    public float getTemperature()
    {
        return 0;
    }

    @Override
    public void setTemperature(float temperature)
    {
        if (temperature > 0f)
        {
            for (ItemStack stack : Helpers.iterate(inventory))
            {
                final IHeat heat = HeatCapability.get(stack);
                if (heat != null)
                {
                    HeatCapability.addTemp(heat, temperature);
                    markForSync();
                }
            }
        }
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
    public Node getRotationNode()
    {
        return node;
    }

    /**
     * @return The angle, in radians, about which the blowpipe in {@code slot} is rotated around the wheel's vertical axis when
     * rendered. The {@code +1}/{@code +3} offset depends on the sign of rotation to keep the blowpipe drawn on the side it is
     * operated from, since an operation fires as a slot passes a device and which edge it arrives from flips with direction.
     */
    public float getSlotRenderAngle(int slot, float partialTick)
    {
        final Rotation rotation = node.rotation();
        final boolean positive = rotation != null && rotation.speed() > 0;
        return -getRotationAngle(partialTick) + Mth.HALF_PI * (slot + (positive ? 1 : 3));
    }

    /**
     * @return The blowpipe slot currently displayed on the given horizontal {@code direction} of the wheel, or {@code -1} if
     * {@code direction} is not horizontal. Computed by mirroring the renderer's transform exactly (rather than re-deriving the
     * mapping), so manual insertion is deterministic: a blowpipe lands in the slot the player sees on the side they clicked.
     */
    public int getSlotForDirection(Direction direction)
    {
        if (direction.getAxis().isVertical())
            return -1;
        for (int slot = 0; slot < SLOTS; slot++)
        {
            // Reproduce where the renderer places this slot: a +Z (south) offset rotated about Y by the slot's render angle.
            final Vector3f offset = new Vector3f(0f, 0f, 1f).rotate(Axis.YP.rotation(getSlotRenderAngle(slot, 0f)));
            final Direction facing = Math.abs(offset.x()) > Math.abs(offset.z())
                ? (offset.x() > 0 ? Direction.EAST : Direction.WEST)
                : (offset.z() > 0 ? Direction.SOUTH : Direction.NORTH);
            if (facing == direction)
                return slot;
        }
        return -1;
    }

    private static final float SPIN_HALF_ARC = Mth.HALF_PI * 0.35f;

    /**
     * @return The angle, in radians, to spin the blowpipe in {@code slot} about its own axis. Computed purely from the render
     * angle, so it stays locked to the drawn position with no server/client phase or reload drift. The blowpipe rolls one full
     * turn as its glass end sweeps a short arc centered on an adjacent device, and is still otherwise - so each pass is one
     * distinct spin. The arc ends map to {@code 0} and {@code TWO_PI} (the same orientation), so the roll is seamless.
     */
    public float getWorkingSpin(int slot, float partialTick)
    {
        if (level == null)
            return 0f;
        final float glassAngle = getSlotRenderAngle(slot, partialTick) + (float) 0;
        if (!(level.getBlockState(getBlockPos().relative(directionFromAngle(glassAngle))).getBlock() instanceof IGlassworkingDevice))
            return 0f;
        // Angular distance of the glass end from the cardinal it points at, in [-HALF_PI/2, HALF_PI/2].
        final float past = glassAngle - Mth.HALF_PI * Math.round(glassAngle / Mth.HALF_PI);
        if (Math.abs(past) > SPIN_HALF_ARC)
            return 0f;
        return (past / SPIN_HALF_ARC * 0.5f + 0.5f) * Mth.TWO_PI;
    }
}
