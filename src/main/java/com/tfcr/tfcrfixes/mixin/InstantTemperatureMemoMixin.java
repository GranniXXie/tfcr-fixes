package com.tfcr.tfcrfixes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * TFCR performance fix: memoize OverworldClimateModel#getInstantTemperature.
 *
 * Spark profiling (x5ot6oqo6B, render thread) showed ~6.7% of total frame time
 * inside getInstantTemperature, almost all of it in
 * calculateDailyTemperature -> SolarCalculator.getSunBasedDayTime /
 * getSunPosition -> getLatitude, which with TFC Real World installed runs an
 * EqualEarth map projection (StrictMath.asin/acos) per call.
 *
 * The killer caller is vanilla sky/fog colour sampling: ClientLevel.getSkyColor
 * and FogRenderer.setupColor each run CubicSampler.gaussianSampleVec3, which
 * fetches the climate colour ~125 times per frame, per colour. Within one tick
 * every input to getInstantTemperature (calendar ticks, days-in-month, block
 * position) is effectively constant for a given sample point, so we cache
 * results per calendar tick on a 4-block position grid and drop the cache the
 * moment the tick changes. 4-block quantisation moves the sampled temperature
 * by a negligible amount (latitude gradient ~0.006 degC, elevation < 0.1 degC)
 * while collapsing the per-frame sample storm into a handful of computations.
 *
 * ThreadLocal because both the client render/game threads and the server
 * thread call into this method (food decay, snow/ice, crops).
 */
@Mixin(targets = "net.dries007.tfc.util.climate.OverworldClimateModel", remap = false)
public abstract class InstantTemperatureMemoMixin {

    private static final ThreadLocal<Map<Long, Float>> tfcr_fixes$tempCache = ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<long[]> tfcr_fixes$tempKey = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, -1});

    @Inject(method = "getInstantTemperature", at = @At("HEAD"), cancellable = true)
    private void tfcr_fixes$getInstantTemperatureHead(LevelReader level, BlockPos pos, long calendarTicks, int daysInMonth, CallbackInfoReturnable<Float> cir) {
        long[] state = tfcr_fixes$tempKey.get();
        Map<Long, Float> cache = tfcr_fixes$tempCache.get();
        if (state[0] != calendarTicks || state[1] != daysInMonth) {
            cache.clear();
            state[0] = calendarTicks;
            state[1] = daysInMonth;
            return;
        }
        Float cached = cache.get(tfcr_fixes$posKey(pos));
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getInstantTemperature", at = @At("RETURN"))
    private void tfcr_fixes$getInstantTemperatureReturn(LevelReader level, BlockPos pos, long calendarTicks, int daysInMonth, CallbackInfoReturnable<Float> cir) {
        long[] state = tfcr_fixes$tempKey.get();
        Map<Long, Float> cache = tfcr_fixes$tempCache.get();
        if (state[0] != calendarTicks || state[1] != daysInMonth) {
            cache.clear();
            state[0] = calendarTicks;
            state[1] = daysInMonth;
        }
        cache.put(tfcr_fixes$posKey(pos), cir.getReturnValue());
    }

    private static long tfcr_fixes$posKey(BlockPos pos) {
        // 4-block grid; X/Z cover the full world border, Y covers -8192..+8191
        long qx = (long)(pos.getX() >> 2) & 0x1FFFFFFL;      // 25 bits
        long qz = (long)(pos.getZ() >> 2) & 0x1FFFFFFL;      // 25 bits
        long qy = ((long)(pos.getY() >> 2) + 2048L) & 0x3FFFL; // 14 bits
        return (qx << 39) | (qz << 14) | qy;
    }
}
