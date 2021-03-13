/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.entities;

import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.types.Wood;
import net.dries007.tfc.mixin.entity.EntityTypeAccessor;
import net.dries007.tfc.util.Helpers;

import static net.dries007.tfc.TerraFirmaCraft.MOD_ID;

@SuppressWarnings("unused")
public class TFCEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, MOD_ID);

    public static final RegistryObject<EntityType<TFCFallingBlockEntity>> FALLING_BLOCK = register("falling_block", EntityType.Builder.<TFCFallingBlockEntity>of(TFCFallingBlockEntity::new, EntityClassification.MISC).sized(0.98f, 0.98f));

    public static final Map<Wood.Default, RegistryObject<EntityType<BoatEntity>>> BOATS = Helpers.mapOfKeys(Wood.Default.class, wood ->
        register(wood.name().toLowerCase() + "_boat", EntityType.Builder.of((factory, classification) -> new TFCBoatEntity(factory, classification, wood), EntityClassification.MISC))
    );

    public static <E extends Entity> RegistryObject<EntityType<E>> register(String name, EntityType.Builder<E> builder)
    {
        return register(name, builder, true);
    }

    public static <E extends Entity> RegistryObject<EntityType<E>> register(String name, EntityType.Builder<E> builder, boolean serialize)
    {
        return ENTITIES.register(name, () -> {
            // This is a hack to avoid the data fixer lookup and error message when it can't find one
            // This could be resolved by MinecraftForge#7636 which would put it behind a config option - hopefully, defaulting to true.
            final String id = MOD_ID + ":" + name;
            final EntityType<E> type = builder.noSave().build(id);
            ((EntityTypeAccessor) type).accessor$setSerialize(serialize);
            return type;
        });
    }
}