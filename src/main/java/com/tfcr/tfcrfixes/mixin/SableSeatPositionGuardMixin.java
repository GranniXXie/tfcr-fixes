package com.tfcr.tfcrfixes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guard for Sable's Create-seat compatibility hook (official-server incident,
 * TFCR 4.1.23).
 *
 * Create seats auto-seat any entity that falls onto them
 * (SeatBlock.updateEntityAfterFallOn -> sitDown). Sable redirects the
 * blockPosition() call in that method to entity.getOnPos()
 * (SeatBlockMixin#sable$updateEntityAfterFallOn) so the seat lookup works on
 * physicalized sublevel structures.
 *
 * Bug: when the supporting-position lookup goes wrong (entity carried by a
 * bobbing/moving structure, stale or plot-space onPos), the returned BlockPos
 * can be out of the world or far from the entity. sitDown() then spawns the
 * SeatEntity at that garbage position and force-rides the player into it —
 * players get teleported below the world and die to the void
 * ("卡出世界 / fell out of the world").
 *
 * Guard: validate the redirected position. It must be inside world build
 * height and within a few blocks of the entity. Anything else falls back to
 * the vanilla entity.blockPosition(), which simply finds no seat and skips
 * the sit — the player splashes into the river instead of dying in the void.
 */
@Mixin(targets = "dev.ryanhcode.sable.neoforge.mixin.compatibility.create.entity_falls_on_block.SeatBlockMixin", remap = false)
public abstract class SableSeatPositionGuardMixin {

    private static final int MAX_HORIZONTAL_DRIFT = 16;

    @Inject(method = "sable$updateEntityAfterFallOn", at = @At("RETURN"), cancellable = true, remap = false)
    private void tfcr_fixes$guardSeatPosition(Entity instance, CallbackInfoReturnable<BlockPos> cir) {
        BlockPos ret = cir.getReturnValue();
        BlockPos fallback = instance.blockPosition();
        if (ret == null) {
            cir.setReturnValue(fallback);
            return;
        }
        int minY = instance.level().getMinBuildHeight();
        int maxY = instance.level().getMaxBuildHeight();
        if (ret.getY() < minY || ret.getY() > maxY + 16) {
            cir.setReturnValue(fallback);
            return;
        }
        if (ret.distManhattan(fallback) > MAX_HORIZONTAL_DRIFT) {
            cir.setReturnValue(fallback);
        }
    }
}
