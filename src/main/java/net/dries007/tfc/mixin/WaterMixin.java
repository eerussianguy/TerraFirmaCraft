/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.mixin;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.util.Helpers;

@Mixin(WaterFluid.class)
public class WaterMixin
{
    @Inject(method = "isSame", at = @At("HEAD"), cancellable = true)
    private void inject$isSame(Fluid fluid, CallbackInfoReturnable<Boolean> cir)
    {
        if (Helpers.isFluid(fluid, TFCTags.Fluids.ANY_INFINITE_WATER))
        {
            cir.setReturnValue(true);
        }
    }
}
