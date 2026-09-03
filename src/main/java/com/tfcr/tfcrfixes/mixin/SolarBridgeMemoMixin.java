package com.tfcr.tfcrfixes.mixin;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.client.overworld.SkyPos;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TFC's ClientSolarCalculatorBridge computes real-world solar geometry
 * (SolarCalculator.getSunBasedDayTime / getSunPosition) with trigonometry on
 * EVERY call, and vanilla sky rendering calls it many times per frame
 * (getSkyDarken, sky colour, fog, sun/moon sprites...). With TFC Real World
 * installed the latitude projection adds even more trig on top.
 * Spark profiling (4iT6ZRBUOT) showed ~3.5 s of StrictMath.asin/acos over a
 * 321 s client profile from these paths alone.
 *
 * All inputs (player Z, hemisphere scale, calendar fractions, world day time)
 * are constant within a single frame, so we memoize the last call and replay
 * it when called again with identical inputs. No staleness is possible: any
 * input change (next frame, player moved, calendar advanced) recomputes.
 */
@Mixin(targets = "net.dries007.tfc.client.overworld.ClientSolarCalculatorBridge", remap = false)
public abstract class SolarBridgeMemoMixin {
    // ---- getDayTime(LevelAccessor) cache ----
    private static LevelAccessor tfcr_fixes$dtLevel;
    private static int tfcr_fixes$dtZ;
    private static float tfcr_fixes$dtScale;
    private static float tfcr_fixes$dtYear;
    private static float tfcr_fixes$dtDayFrac;
    private static long tfcr_fixes$dtWorldTime;
    private static long tfcr_fixes$dtResult;
    private static boolean tfcr_fixes$dtValid;

    // ---- getSunPosition(Level, BlockPos) cache ----
    private static Level tfcr_fixes$spLevel;
    private static int tfcr_fixes$spZ;
    private static float tfcr_fixes$spScale;
    private static float tfcr_fixes$spYear;
    private static float tfcr_fixes$spDayFrac;
    private static SkyPos tfcr_fixes$spResult;
    private static boolean tfcr_fixes$spValid;

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$getDayTimeHead(LevelAccessor level, CallbackInfoReturnable<Long> cir) {
        Player player = ClientHelpers.getPlayer();
        if (player == null || !(level instanceof Level)) {
            tfcr_fixes$dtValid = false;
            return;
        }
        int z = player.blockPosition().getZ();
        float scale = Climate.get((Level) level).hemisphereScale();
        float yr = Calendars.CLIENT.getCalendarFractionOfYear();
        float df = Calendars.CLIENT.getCalendarFractionOfDay();
        long wt = level.getLevelData().getDayTime();
        if (tfcr_fixes$dtValid && tfcr_fixes$dtLevel == level && tfcr_fixes$dtZ == z
            && tfcr_fixes$dtScale == scale && tfcr_fixes$dtYear == yr && tfcr_fixes$dtDayFrac == df
            && tfcr_fixes$dtWorldTime == wt) {
            cir.setReturnValue(tfcr_fixes$dtResult);
        }
    }

    @Inject(method = "getDayTime", at = @At("RETURN"))
    private static void tfcr_fixes$getDayTimeReturn(LevelAccessor level, CallbackInfoReturnable<Long> cir) {
        Player player = ClientHelpers.getPlayer();
        if (player == null || !(level instanceof Level)) {
            tfcr_fixes$dtValid = false;
            return;
        }
        tfcr_fixes$dtLevel = level;
        tfcr_fixes$dtZ = player.blockPosition().getZ();
        tfcr_fixes$dtScale = Climate.get((Level) level).hemisphereScale();
        tfcr_fixes$dtYear = Calendars.CLIENT.getCalendarFractionOfYear();
        tfcr_fixes$dtDayFrac = Calendars.CLIENT.getCalendarFractionOfDay();
        tfcr_fixes$dtWorldTime = level.getLevelData().getDayTime();
        tfcr_fixes$dtResult = cir.getReturnValue();
        tfcr_fixes$dtValid = true;
    }

    @Inject(method = "getSunPosition", at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$getSunPositionHead(Level level, BlockPos pos, CallbackInfoReturnable<SkyPos> cir) {
        int z = pos.getZ();
        float scale = Climate.get(level).hemisphereScale();
        float yr = Calendars.CLIENT.getCalendarFractionOfYear();
        float df = Calendars.CLIENT.getCalendarFractionOfDay();
        if (tfcr_fixes$spValid && tfcr_fixes$spLevel == level && tfcr_fixes$spZ == z
            && tfcr_fixes$spScale == scale && tfcr_fixes$spYear == yr && tfcr_fixes$spDayFrac == df) {
            cir.setReturnValue(tfcr_fixes$spResult);
        }
    }

    @Inject(method = "getSunPosition", at = @At("RETURN"))
    private static void tfcr_fixes$getSunPositionReturn(Level level, BlockPos pos, CallbackInfoReturnable<SkyPos> cir) {
        tfcr_fixes$spLevel = level;
        tfcr_fixes$spZ = pos.getZ();
        tfcr_fixes$spScale = Climate.get(level).hemisphereScale();
        tfcr_fixes$spYear = Calendars.CLIENT.getCalendarFractionOfYear();
        tfcr_fixes$spDayFrac = Calendars.CLIENT.getCalendarFractionOfDay();
        tfcr_fixes$spResult = cir.getReturnValue();
        tfcr_fixes$spValid = true;
    }
}
