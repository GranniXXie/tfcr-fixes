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
 * regenerating it a handful of times per minute is visually indistinguishable.
 *
 * History: the throttle was 100 ms (10 regens/sec), but a single regeneration is
 * so expensive (10k warped-noise evaluations + a GPU texture upload) that even
 * throttled it still burned ~5.6% of the render thread (spark 4iT6ZRBUOT).
 * Bumped to 5000 ms: the plasma animation steps very slightly slower, and the
 * render-thread cost drops to a rounding error (~0.1%).
 */
@Mixin(targets = "dev.devce.rocketnautics.client.SkyHandler", remap = false)
public abstract class StarPlasmaThrottleMixin {
    private static final long TFCR_FIXES$MIN_INTERVAL_MS = 5000L;
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
