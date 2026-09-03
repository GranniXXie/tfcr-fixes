package com.tfcr.tfcrfixes.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFCR fix for an upstream Create 6.0.10 crash that kicks players from servers
 * with "网络协议错误" (network protocol error).
 *
 * PressingBehaviour.spawnParticles() only checks that particleItems is
 * non-empty, never that the individual stacks are. particleItems holds LIVE
 * references to basin input slots / item-entity stacks, so once the recipe
 * consumes them (shrink to 0) the list silently contains EMPTY stacks.
 * saveOptional serializes those as id-less tags, ItemStack.parseOptional reads
 * them back as EMPTY without complaint, and on the client
 * makeCompactingParticleEffect / makePressingParticleEffect feed them straight
 * into `new ItemParticleOption(...)`, whose constructor throws
 * IllegalArgumentException("Empty stacks are not allowed"). NeoForge catches
 * the packet-handling exception and disconnects the player.
 *
 * Guard: skip particle spawning for empty stacks. Pure visual no-op.
 */
@Mixin(targets = "com.simibubi.create.content.kinetics.press.PressingBehaviour", remap = false)
public abstract class PressParticleEmptyGuardMixin {

    @Inject(method = "makeCompactingParticleEffect", at = @At("HEAD"), cancellable = true)
    private void tfcr_fixes$guardCompacting(net.minecraft.world.phys.Vec3 pos, ItemStack stack, CallbackInfo ci) {
        if (stack.isEmpty()) {
            ci.cancel();
        }
    }

    @Inject(method = "makePressingParticleEffect(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void tfcr_fixes$guardPressing(net.minecraft.world.phys.Vec3 pos, ItemStack stack, CallbackInfo ci) {
        if (stack.isEmpty()) {
            ci.cancel();
        }
    }

    @Inject(method = "makePressingParticleEffect(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"), cancellable = true)
    private void tfcr_fixes$guardPressingBulk(net.minecraft.world.phys.Vec3 pos, ItemStack stack, int amount, CallbackInfo ci) {
        if (stack.isEmpty()) {
            ci.cancel();
        }
    }
}
