/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities.ai.prey;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PrepareRamNearestTarget;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.dries007.tfc.common.entities.ai.predator.PredatorAi;
import net.dries007.tfc.common.entities.prey.RammingPrey;

public class PrepareRamNearestTargetTFC<E extends PathfinderMob> extends Behavior<E>
{
    private final ToIntFunction<E> getCooldownOnFail;
    private final float walkSpeed;
    private final int ramPrepareTime;
    private final Function<E, SoundEvent> getPrepareRamSound;
    private Optional<Long> reachedRamPositionTimestamp = Optional.empty();
    private Optional<net.minecraft.world.entity.ai.behavior.PrepareRamNearestTarget.RamCandidate> ramCandidate = Optional.empty();

    public PrepareRamNearestTargetTFC(ToIntFunction<E> coolDownOnFail, float walkSpeed, int ramPrepareTime, Function<E, SoundEvent> prepareRamSound)
    {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT, MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_ABSENT), 160);
        this.getCooldownOnFail = coolDownOnFail;
        this.walkSpeed = walkSpeed;
        this.ramPrepareTime = ramPrepareTime;
        this.getPrepareRamSound = prepareRamSound;
    }

    protected void start(ServerLevel level, PathfinderMob rammingPrey, long time)
    {
        Brain<?> brain = rammingPrey.getBrain();
        final TargetingConditions ramTargeting = ((RammingPrey) rammingPrey).isMale() && !rammingPrey.isBaby() ? RammingPreyAi.RAM_TARGET_CONDITIONS_ADULT_MALE : RammingPreyAi.RAM_TARGET_CONDITIONS;

        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).flatMap((nearestVisibleLivingEntities) -> {
            return nearestVisibleLivingEntities.findClosest((nearestVisibleEntity) -> {
                return ramTargeting.test(rammingPrey, nearestVisibleEntity);
            });
        }).ifPresent((target) -> {
            this.chooseRamPosition(rammingPrey, target);
        });
    }

    protected void stop(ServerLevel level, E rammingPrey, long time)
    {
        Brain<?> brain = rammingPrey.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.RAM_TARGET))
        {
            level.broadcastEntityEvent(rammingPrey, (byte) 59);
            brain.setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getCooldownOnFail.applyAsInt(rammingPrey));
        }

    }

    protected boolean canStillUse(ServerLevel level, PathfinderMob rammingPrey, long time)
    {
        return this.ramCandidate.isPresent() && this.ramCandidate.get().getTarget().isAlive();
    }

    protected void tick(ServerLevel level, E rammingPrey, long time)
    {
        if (!this.ramCandidate.isEmpty())
        {
            rammingPrey.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.ramCandidate.get().getStartPosition(), this.walkSpeed, 0));
            rammingPrey.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.ramCandidate.get().getTarget(), true));
            //This checks if the target position and the target entity are NOT different
            boolean flag = !this.ramCandidate.get().getTarget().blockPosition().equals(this.ramCandidate.get().getTargetPosition());
            if (flag)
            {
                //If the target has moved, update position
                if (PredatorAi.hasNearbyAttacker(rammingPrey))
                {
                    level.broadcastEntityEvent(rammingPrey, (byte) 58);
                }
                else
                {
                    level.broadcastEntityEvent(rammingPrey, (byte) 59);
                }
                rammingPrey.getNavigation().stop();
                this.chooseRamPosition(rammingPrey, (this.ramCandidate.get()).getTarget());
            }
            else
            {
                BlockPos blockpos = rammingPrey.blockPosition();
                if (blockpos.equals(this.ramCandidate.get().getStartPosition()))
                {
                    level.broadcastEntityEvent(rammingPrey, (byte) 58);
                    if (this.reachedRamPositionTimestamp.isEmpty())
                    {
                        this.reachedRamPositionTimestamp = Optional.of(time);
                    }

                    if (time - this.reachedRamPositionTimestamp.get() >= (long) this.ramPrepareTime)
                    {
                        if (rammingPrey instanceof RammingPrey)
                        {
                            ((RammingPrey) rammingPrey).setAttackDamageMultiplier(calcRamDamageMultiplier(blockpos, this.ramCandidate.get().getTargetPosition()));
                        }
                        //This line here actually starts the ram
                        rammingPrey.getBrain().setMemory(MemoryModuleType.RAM_TARGET, this.getEdgeOfBlock(blockpos, this.ramCandidate.get().getTargetPosition()));
                        level.playSound((Player) null, rammingPrey, this.getPrepareRamSound.apply(rammingPrey), SoundSource.NEUTRAL, 1.0F, rammingPrey.getVoicePitch());

                        this.ramCandidate = Optional.empty();
                        //This should always reset after ramming
                        this.reachedRamPositionTimestamp = Optional.empty();
                    }
                }
            }
        }
    }

    private float calcRamDamageMultiplier(BlockPos startPos, BlockPos finishPos)
    {
        final float dist = (float) startPos.distSqr(finishPos);
        final int maxRamDistance = RammingPreyAi.RAM_MAX_DISTANCE;
        return Mth.clamp(1.2f * dist / maxRamDistance, 0.25f, 2.0f);
    }

    private Vec3 getEdgeOfBlock(BlockPos pos1, BlockPos pos2)
    {
        double d0 = 0.5D;
        double d1 = 0.5D * (double) Mth.sign((double) (pos2.getX() - pos1.getX()));
        double d2 = 0.5D * (double) Mth.sign((double) (pos2.getZ() - pos1.getZ()));
        return Vec3.atBottomCenterOf(pos2).add(d1, 0.0D, d2);
    }

    private void chooseRamPosition(PathfinderMob rammingPrey, LivingEntity target)
    {
        //Modified from Vanilla behavior. Just use the current position as the starting position
        this.ramCandidate = Optional.of(new PrepareRamNearestTarget.RamCandidate(rammingPrey.blockPosition(), target.blockPosition(), target));
    }
}
