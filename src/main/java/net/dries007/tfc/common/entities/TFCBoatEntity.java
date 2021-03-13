package net.dries007.tfc.common.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.types.Wood;

public class TFCBoatEntity extends BoatEntity
{
    private final Wood.Default wood;

    public TFCBoatEntity(EntityType<? extends BoatEntity> type, World worldIn, Wood.Default wood)
    {
        super(type, worldIn);
        this.wood = wood;
    }

    public TFCBoatEntity(World worldIn, double x, double y, double z, Wood.Default wood)
    {
        this(TFCEntities.BOATS.get(wood).get(), worldIn, wood);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vector3d.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public Item getDropItem()
    {
        return TFCItems.BOATS.get(wood).get();
    }
}
