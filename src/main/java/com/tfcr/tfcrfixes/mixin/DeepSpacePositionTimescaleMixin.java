package com.tfcr.tfcrfixes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cosmonautics (rocketnautics) defaults every deep-space instance's timescale to 1,
 * set in the constructor and in reset(). For TFCR we want space to run at 2x by
 * default, so we overwrite the field right after both. Instances loaded from saved
 * data call read() after construction, which restores the stored value, so explicit
 * `/rn timescale` settings are still respected; only never-touched instances get 2.
 */
@Mixin(targets = "dev.devce.rocketnautics.content.orbit.universe.DeepSpacePosition", remap = false)
public abstract class DeepSpacePositionTimescaleMixin {
    @Shadow
    private int timescale;

    @Inject(method = {"<init>", "reset"}, at = @At("RETURN"))
    private void tfcr_fixes$defaultTimescaleTwo(CallbackInfo ci) {
        this.timescale = 2;
    }
}
