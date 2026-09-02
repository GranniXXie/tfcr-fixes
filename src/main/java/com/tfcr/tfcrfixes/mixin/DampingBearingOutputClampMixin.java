package com.tfcr.tfcrfixes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clamps the Create: Aeronautics Transmission Linkage damping stress bearing
 * (动能转化轴承) output RPM to its rated 256.
 *
 * The bearing translates relative angular velocity between two physics bodies
 * straight into Create network speed with rounding but no ceiling. Whenever the
 * parent-body lookup drops out for a tick (chunk resync, assembly transition)
 * the relative speed momentarily equals the ship's full spin, and physics
 * impulses (collisions, constraint snaps) spike it further — players saw
 * momentary "雷霆巨力" stress bursts and unstable networks. Clamping at the
 * rated speed keeps legitimate output identical while capping glitch spikes.
 */
@Mixin(targets = "com.enxv.aerouniversaljoint.content.DampingStressBearingBlockEntity", remap = false)
public abstract class DampingBearingOutputClampMixin {

    private static final float MAX_OUTPUT_RPM = 256.0f;

    @Inject(method = "normalizeOutputSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private void tfcr_fixes$clampOutputRpm(float rpm, CallbackInfoReturnable<Float> cir) {
        float value = cir.getReturnValue();
        if (value > MAX_OUTPUT_RPM) {
            cir.setReturnValue(MAX_OUTPUT_RPM);
        } else if (value < -MAX_OUTPUT_RPM) {
            cir.setReturnValue(-MAX_OUTPUT_RPM);
        }
    }
}
