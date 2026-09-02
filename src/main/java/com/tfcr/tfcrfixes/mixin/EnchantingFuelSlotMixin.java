package com.tfcr.tfcrfixes.mixin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge 21.1 no longer patches the enchanting table fuel slot to use the
 * neoforge:enchanting_fuels tag (EnchantmentMenu$3#mayPlace checks
 * Items.LAPIS_LAZULI directly). Re-open the slot to TFC gem powders so the
 * pack's enchanting fuel variety works. Lapis itself keeps working via the
 * original return value.
 */
@Mixin(targets = "net.minecraft.world.inventory.EnchantmentMenu$3")
public abstract class EnchantingFuelSlotMixin {

    private static final TagKey<Item> GEM_POWDERS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tfc", "gem_powders"));

    @Inject(method = "mayPlace", at = @At("RETURN"), cancellable = true)
    private void tfcr_fixes$gemPowderFuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            cir.setReturnValue(stack.is(GEM_POWDERS));
        }
    }
}
