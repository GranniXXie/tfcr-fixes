package com.tfcr.tfcrfixes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cosmonautics (rocketnautics) regenerates its animated "star plasma" noise texture
 * through SkyHandler.ensureStarPlasmaTexture on practically every rendered frame,
 * even in the overworld with enableCustomSky = false. Spark profiling showed
 * StarNoise.warpedNoise/fbm burning ~18-20% of the render thread in a fresh
 * overworld save.
 *
 * The texture is only consumed by the custom sky renderer (space visuals), so
 * regenerating it at most 10 times per second is visually indistinguishable.
 * This injector throttles the regeneration by wall clock: calls arriving within
 * 100 ms of the last accepted regeneration are skipped, leaving the previously
 * generated texture in place.
 */
@Mixin(targets = "dev.devce.rocketnautics.client.SkyHandler", remap = false)
public abstract class StarPlasmaThrottleMixin {
    private static final long TFCR_FIXES$MIN_INTERVAL_MS = 100L;
    private static long tfcr_fixes$lastAcceptedRun = Long.MIN_VALUE / 2;

    @Inject(method = "ensureStarPlasmaTexture", at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$throttleStarPlasma(CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (now - tfcr_fixes$lastAcceptedRun < TFCR_FIXES$MIN_INTERVAL_MS) {
            ci.cancel();
        } else {
            tfcr_fixes$lastAcceptedRun = now;
        }
    }
}
