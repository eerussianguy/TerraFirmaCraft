/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.util.events;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.devices.FirepitBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.InteractionManager;
import net.dries007.tfc.util.advancements.TFCAdvancements;

/**
 * This event is used for lighting things with fire. It can be cancelled to handle lighting of an external device or source.
 * <p>
 * When the strength of the event is {@link #isStrong()}, if it is <strong>not</strong> cancelled, a fire block will be created. If this was cancelled, the {@link TFCAdvancements#LIT} will be triggered.
 * <p>
 * For simple devices that create fires either by right-clicking (like flint and steel) or by consuming (like fire charges), they can be added to the tags
 * {@code #tfc:starts_fires_with_durability} or {@code #tfc:starts_fires_with_items} and this event will be fired from {@link InteractionManager} automatically.
 */
public final class StartFireEvent extends Event implements ICancellableEvent
{
    // Replicate vanilla fire starting sounds that do not fire
    public static boolean startFireWithSound(Level level, BlockPos pos, BlockState state, Direction direction, @Nullable Player player, ItemStack stack)
    {
        final boolean startFire = startFire(level, pos, state, direction, player, stack, FireStrength.STRONG, 1.0);
        if (startFire)
        {
            if (stack.getItem() instanceof FlintAndSteelItem)
            {
                level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            }
            else if (stack.getItem() instanceof FireChargeItem)
            {
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F + 1.0F);
            }

        }
        return startFire;
    }

    public static boolean startFire(Level level, BlockPos pos, BlockState state, Direction direction, @Nullable Player player, ItemStack stack)
    {
        return startFire(level, pos, state, direction, player, stack, FireStrength.STRONG, 1.0);
    }

    // Pass in a firepitBaseChance of -1 to disable firepit creation for a given firestarter
    public static boolean startFire(Level level, BlockPos pos, BlockState state, Direction direction, @Nullable Player player, ItemStack stack, FireStrength strength, double firepitBaseChance)
    {
        final BlockPos relativePos = pos.relative(direction);
        final BlockState relativeState = level.getBlockState(relativePos);
        if (!relativeState.getFluidState().isEmpty())
        {
            return false;
        }
        final StartFireEvent event = new StartFireEvent(level, pos, state, direction, player, stack, strength);
        final boolean cancelled = NeoForge.EVENT_BUS.post(event).isCanceled();

        if (cancelled && player instanceof ServerPlayer serverPlayer)
        {
            TFCAdvancements.LIT.trigger(serverPlayer, state);
        }

        if (!cancelled && event.isStrong())
        {
            // Check conditions for creating a firepit if a valid firestarter
            if (FirepitBlock.canSurvive(level, relativePos) && firepitBaseChance != -1)
            {
                final List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(relativePos.getX() - 0.5, relativePos.getY(), relativePos.getZ() - 0.5, relativePos.getX() + 1.5, relativePos.getY() + 1, relativePos.getZ() + 1.5));
                final List<ItemEntity> usableItems = new ArrayList<>();

                int sticks = 0, kindling = 0;
                final List<ItemStack> initialLogs = new ArrayList<>();

                for (ItemEntity entity : items)
                {
                    ItemStack foundStack = entity.getItem();
                    Item foundItem = foundStack.getItem();
                    int itemCount = foundStack.getCount();
                    if (Helpers.isItem(foundItem, TFCTags.Items.FIREPIT_STICKS))
                    {
                        sticks += itemCount;
                        usableItems.add(entity);
                    }
                    else if (Helpers.isItem(foundItem, TFCTags.Items.FIREPIT_KINDLING))
                    {
                        kindling += itemCount;
                        usableItems.add(entity);
                    }
                    // 4 is the amount of fuel slots in AbstractFirepitBlockEntity, from 0 to 3
                    else if (initialLogs.size() < 4 && Helpers.isItem(foundItem, TFCTags.Items.FIREPIT_LOGS))
                    {
                        for (int i = 0; i < itemCount; i++)
                        {
                            if (initialLogs.size() >= 4) break;
                            initialLogs.add(foundItem.getDefaultInstance());
                        }
                        usableItems.add(entity);
                    }
                }
                if (sticks >= 3 && !initialLogs.isEmpty())
                {
                    final float kindlingModifier = Math.min(0.1F * (float) kindling, 0.5F);
                    if (level.random.nextFloat() < firepitBaseChance + kindlingModifier)
                    {
                        usableItems.forEach(Entity::kill);

                        final BlockState firepitState;
                        if (player != null)
                        {
                            firepitState = TFCBlocks.FIREPIT.get().defaultBlockState().setValue(FirepitBlock.AXIS, player.getDirection().getAxis());
                        }
                        else
                        {
                            firepitState = TFCBlocks.FIREPIT.get().defaultBlockState().setValue(FirepitBlock.AXIS, Direction.Axis.X);
                        }
                        level.setBlock(relativePos, firepitState, 3);
                        level.getBlockEntity(relativePos, TFCBlockEntities.FIREPIT.get()).ifPresent(firepit -> {
                            for (int slot = 0; slot < initialLogs.size(); slot++)
                            {
                                firepit.getInventory().setStackInSlot(slot, initialLogs.get(slot));
                            }
                            firepit.light(firepitState);
                        });
                        if (player instanceof ServerPlayer serverPlayer)
                        {
                            TFCAdvancements.FIREPIT_CREATED.trigger(serverPlayer, firepitState);
                        }
                        return true;
                    }
                    return false;
                }
            }
            // If the block we are targeting is a non-solid block, we delete the block and replace it with fire
            // Otherwise with solid blocks, we place fire on the face we were targeting
            if (state.isCollisionShapeFullBlock(level, pos))
            {
                pos = pos.relative(direction);
            }
            else
            {
                final BlockState stateAt = level.getBlockState(pos);
                if (stateAt.isFlammable(level, pos, direction) && (stateAt.canBeReplaced() || stateAt.getCollisionShape(level, pos).isEmpty()))
                {
                    level.destroyBlock(pos, false);
                }
            }

            if (BaseFireBlock.canBePlacedAt(level, pos, direction))
            {
                level.setBlock(pos, BaseFireBlock.getState(level, pos), 11);
                return true;
            }
        }
        return cancelled;
    }

    private final Level world;
    private final BlockPos pos;
    private final BlockState state;
    private final Direction direction;
    private final @Nullable Player player;
    private final ItemStack stack;
    private final FireStrength strength;

    private StartFireEvent(Level world, BlockPos pos, BlockState state, Direction direction, @Nullable Player player, ItemStack stack, FireStrength strength)
    {
        this.world = world;
        this.pos = pos;
        this.state = state;
        this.direction = direction;
        this.player = player;
        this.stack = stack;
        this.strength = strength;
    }

    public Level getLevel()
    {
        return world;
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public BlockState getState()
    {
        return state;
    }

    public Direction getTargetedFace()
    {
        return direction;
    }

    @Nullable
    public Player getPlayer()
    {
        return player;
    }

    public ItemStack getItemStack()
    {
        return stack;
    }

    /**
     * @return If the fire starting was <strong>strong</strong>, and is likely to cause destructive behavior like lighting fires.
     */
    public boolean isStrong()
    {
        return strength == FireStrength.STRONG;
    }

    public enum FireStrength
    {
        /**
         * Strong represents a fire starting where:
         * 1. The fire starting was the primary functionality (i.e. not a side effect). This is to make it obvious to the player what can happen, or
         * 2. Destructive fire starting behaviors (such as creating fire blocks, lighting log piles, etc.) is desired.
         */
        STRONG,
        /**
         * Weak represents a fire starting where:
         * 1. The fire starting may have been secondary behavior or a side effect (i.e. easy to misclick).
         * 2. Destructive fire starting behaviors should <strong>not</strong> be attempted.
         */
        WEAK
        // More granularity may be added if needed
    }
}
