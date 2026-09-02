package com.tfcr.tfcrfixes.mixin;

import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create 6.0.10 contraption collision NPE guard.
 *
 * ContinuousOBBCollider.collideMany reads {@code mf.axis} when a discrete
 * collision is detected and stepSeparation > entityMaxStep. However
 * ContinuousSeparationManifold.axis is only assigned inside separate() under
 * specific conditions (distance != 0 and a smaller separation than previously
 * recorded). In the degenerate edge case — typically an entity perfectly
 * aligned with the tested separation axes (distance == 0), which tilted
 * Aeronautics/Sable contraptions make much more likely — axis is never
 * assigned while isDiscreteCollision stays true, so the read throws:
 *
 *   NullPointerException: Cannot read field "x" because "mf.axis" is null
 *     at ContinuousOBBCollider.collideMany
 *     at ContraptionCollider.collideEntities
 *
 * Observed in TFCR 4.1.21 with a chain-conveyor contraption carrying a drill
 * after the player changed its rotation speed. Upstream has no fix as of
 * 6.0.10 (latest 1.21.1 release).
 *
 * Defensive approach: keep {@code axis}/{@code normalAxis} non-null by
 * substituting Vec3.ZERO at construction and at reset(). A zero axis makes
 * the affected response a no-op (adds 0 * separation) instead of crashing.
 * No code path checks these fields for null, so behavior is unchanged in
 * every non-crashing scenario.
 */
@Mixin(targets = "com.simibubi.create.foundation.collision.ContinuousOBBCollider$ContinuousSeparationManifold", remap = false)
public abstract class CreateObbManifoldNullGuardMixin {

    @Shadow(remap = false)
    private Vec3 axis;

    @Shadow(remap = false)
    private Vec3 normalAxis;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tfcr_fixes$initAxesNonNull(CallbackInfo ci) {
        if (this.axis == null) this.axis = Vec3.ZERO;
        if (this.normalAxis == null) this.normalAxis = Vec3.ZERO;
    }

    @Inject(method = "reset", at = @At("RETURN"), remap = false)
    private void tfcr_fixes$resetAxesNonNull(CallbackInfo ci) {
        this.axis = Vec3.ZERO;
        this.normalAxis = Vec3.ZERO;
    }
}
