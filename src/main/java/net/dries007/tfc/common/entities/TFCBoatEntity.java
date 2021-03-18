package net.dries007.tfc.common.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.types.Wood;

public class TFCBoatEntity extends BoatEntity
{
    public static final DataParameter<Integer> TFC_WOOD_ID = EntityDataManager.defineId(BoatEntity.class, DataSerializers.INT);

    public TFCBoatEntity(EntityType<? extends BoatEntity> type, World worldIn)
    {
        super(type, worldIn);
    }

    public TFCBoatEntity(World worldIn, double x, double y, double z)
    {
        this(TFCEntities.BOAT.get(), worldIn);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vector3d.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public void setWood(Wood.Default wood)
    {
        entityData.set(TFC_WOOD_ID, wood.ordinal());
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(TFC_WOOD_ID, 0);
    }

    @Override
    public Item getDropItem()
    {
        return TFCItems.BOATS.get(Wood.Default.valueOf(entityData.get(TFC_WOOD_ID))).get();
    }
}
