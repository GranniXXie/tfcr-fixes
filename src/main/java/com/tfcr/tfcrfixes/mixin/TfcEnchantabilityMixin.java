package com.tfcr.tfcrfixes.mixin;

import com.tfcr.tfcrfixes.EnchantabilityBootstrap;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores nonzero enchantability for TFC metal armor/weapons so the
 * enchanting table accepts them. Vanilla default is 0 ("cannot enchant");
 * items not in the lookup map keep vanilla behavior.
 */
@Mixin(Item.class)
public abstract class TfcEnchantabilityMixin {

    @Inject(method = "getEnchantmentValue", at = @At("RETURN"), cancellable = true)
    private void tfcr_fixes$tfcEnchantability(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() == 0) {
            Integer value = EnchantabilityBootstrap.lookup((Item) (Object) this);
            if (value != null) {
                cir.setReturnValue(value);
            }
        }
    }
}
