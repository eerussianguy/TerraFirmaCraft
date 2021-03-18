package net.dries007.tfc.common.items;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import net.dries007.tfc.common.entities.TFCBoatEntity;
import net.dries007.tfc.common.types.Wood;

public class TFCBoatItem extends Item
{
    private static final Predicate<Entity> ENTITY_PREDICATE = EntityPredicates.NO_SPECTATORS.and(Entity::isPickable);

    private final Wood.Default wood;

    public TFCBoatItem(Properties properties, Wood.Default wood)
    {
        super(properties);
        this.wood = wood;
    }

    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn)
    {
        ItemStack held = playerIn.getItemInHand(handIn);
        RayTraceResult raytraceresult = getPlayerPOVHitResult(worldIn, playerIn, RayTraceContext.FluidMode.ANY);
        if (raytraceresult.getType() == RayTraceResult.Type.MISS)
        {
            return ActionResult.pass(held);
        }
        else
        {
            Vector3d vector3d = playerIn.getViewVector(1.0F);
            List<Entity> list = worldIn.getEntities(playerIn, playerIn.getBoundingBox().expandTowards(vector3d.scale(5.0D)).inflate(1.0D), ENTITY_PREDICATE);
            if (!list.isEmpty())
            {
                Vector3d vector3d1 = playerIn.getEyePosition(1.0F);
                for (Entity entity : list)
                {
                    AxisAlignedBB axisalignedbb = entity.getBoundingBox().inflate(entity.getPickRadius());
                    if (axisalignedbb.contains(vector3d1))
                    {
                        return ActionResult.pass(held);
                    }
                }
            }

            if (raytraceresult.getType() == RayTraceResult.Type.BLOCK)
            {
                Vector3d location = raytraceresult.getLocation();
                TFCBoatEntity boat = new TFCBoatEntity(worldIn, location.x, location.y, location.z);
                boat.setWood(wood);
                boat.yRot = playerIn.yRot;
                if (!worldIn.noCollision(boat, boat.getBoundingBox().inflate(-0.1D)))
                {
                    return ActionResult.fail(held);
                }
                else
                {
                    if (!worldIn.isClientSide)
                    {
                        worldIn.addFreshEntity(boat);
                        if (!playerIn.abilities.instabuild)
                        {
                            held.shrink(1);
                        }
                    }
                    playerIn.awardStat(Stats.ITEM_USED.get(this));
                    return ActionResult.sidedSuccess(held, worldIn.isClientSide());
                }
            }
            else
            {
                return ActionResult.pass(held);
            }
        }
    }
}
