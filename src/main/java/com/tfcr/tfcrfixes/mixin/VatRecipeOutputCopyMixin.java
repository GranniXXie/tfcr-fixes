package com.tfcr.tfcrfixes.mixin;

import java.util.Optional;

import com.eerussianguy.firmalife.common.blockentities.VatBlockEntity;
import com.eerussianguy.firmalife.common.recipes.VatRecipe;
import net.dries007.tfc.common.recipes.outputs.ItemStackProvider;
import net.dries007.tfc.util.Helpers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Firmalife's VatRecipe.assembleOutputs mutates the recipe-cached output stacks:
 *
 *  - jarOutput: setCount() is called on the cached ItemStack, and both
 *    VatBlockEntity.takeOutput() and the jarring station then split() that SAME
 *    instance. Once the player removes the first jar, the recipe's cached stack is
 *    drained to count 0 (= EMPTY), so every later cook consumes ingredients and
 *    produces nothing - the dreaded "void vat" (first craft per session works,
 *    because recipes are rebuilt on datapack reload).
 *
 *  - outputFluid: setAmount() is called on the cached FluidStack, permanently
 *    inflating the recipe's output amount (500mb sugar water becomes 5000/10000mb
 *    after one cook), so later cooks duplicate fluid far beyond the recipe ratio.
 *
 * This mixin re-implements assembleOutputs with defensive copies of both outputs.
 * The logic mirrors Firmalife 3.0.14 exactly, except for the copies.
 */
@Mixin(value = VatRecipe.class, remap = false)
public abstract class VatRecipeOutputCopyMixin {

    @Shadow
    @Final
    private SizedIngredient inputItem;
    @Shadow
    @Final
    private SizedFluidIngredient inputFluid;
    @Shadow
    @Final
    private Optional<ItemStackProvider> outputItem;
    @Shadow
    @Final
    private Optional<FluidStack> outputFluid;
    @Shadow
    @Final
    private Optional<ItemStack> jarOutput;
    @Shadow
    @Final
    private Optional<ResourceLocation> outputTexture;

    @Inject(method = "assembleOutputs", at = @At("HEAD"), cancellable = true)
    private void tfcr_fixes$assembleOutputsWithCopies(VatBlockEntity vat, VatBlockEntity.VatInventory inventory, CallbackInfo ci) {
        ci.cancel();

        final ItemStack stack = Helpers.removeStack((IItemHandler) inventory, 0);
        final FluidStack fluid = inventory.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        int multiplier = this.inputItem.count() == 0
            ? fluid.getAmount() / this.inputFluid.amount()
            : (this.inputFluid.amount() == 0
                ? stack.getCount() / this.inputItem.count()
                : Math.min(fluid.getAmount() / this.inputFluid.amount(), stack.getCount() / this.inputItem.count()));

        // FIXED: copy the cached fluid output before reading/mutating its amount
        final FluidStack outputFluidStack = this.outputFluid.map(FluidStack::copy).orElse(FluidStack.EMPTY);
        if (!outputFluidStack.isEmpty()) {
            int capacity = 10000;
            if (FluidStack.isSameFluidSameComponents(outputFluidStack, fluid)) {
                capacity -= fluid.getAmount();
            }
            final int maxMultiplier = capacity / outputFluidStack.getAmount();
            multiplier = Math.min(multiplier, maxMultiplier);
        }

        final ItemStack outputItemStack = this.outputItem.map(provider -> provider.getSingleStack(stack)).orElse(ItemStack.EMPTY);
        if (!outputItemStack.isEmpty()) {
            Helpers.consumeInStackSizeIncrements(outputItemStack, multiplier * outputItemStack.getCount(), inventory::insertItemWithOverflow);
        }

        final int remainingItemCount = stack.getCount() - multiplier * this.inputItem.count();
        if (remainingItemCount > 0) {
            final ItemStack remainingStack = stack.copy();
            remainingStack.setCount(remainingItemCount);
            inventory.insertItemWithOverflow(remainingStack);
        }

        if (outputFluidStack.isEmpty()) {
            final int retainAmount = fluid.getAmount() - multiplier * this.inputFluid.amount();
            if (retainAmount > 0) {
                final FluidStack retainedFluid = fluid.copy();
                retainedFluid.setAmount(retainAmount);
                inventory.fill(retainedFluid, IFluidHandler.FluidAction.EXECUTE);
            }
        } else {
            int amount = outputFluidStack.getAmount() * multiplier;
            if (FluidStack.isSameFluidSameComponents(outputFluidStack, fluid)) {
                amount += fluid.getAmount();
            }
            outputFluidStack.setAmount(Math.min(10000, amount));
            inventory.fill(outputFluidStack, IFluidHandler.FluidAction.EXECUTE);
        }

        // FIXED: copy the cached jar output so takeOutput()/jarring station split()
        // can no longer drain the recipe's stored stack to EMPTY
        final ItemStack jar = this.jarOutput.map(ItemStack::copy).orElse(ItemStack.EMPTY);
        if (!jar.isEmpty()) {
            jar.setCount(jar.getCount() * multiplier);
            vat.setOutput(jar, this.outputTexture.orElse(null));
        }
    }
}
