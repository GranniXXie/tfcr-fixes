package com.tfcr.tfcrfixes.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * tfc_coldsweat hooks TFC's PlayerInfo.getThirstContributionFromTemperature to add
 * a Cold Sweat based temperature contribution. TFC calls this method from its
 * thirst HUD overlay EVERY FRAME, and tfc_coldsweat's implementation runs
 * WorldHelper.getTemperatureAt -> BlockTempModifier.calculate, which raycasts and
 * scans blocks around the player. Spark profiling showed this costing ~21% of the
 * render thread.
 *
 * The HUD does not need per-frame precision, so on the client we cache the computed
 * value for 10 game ticks (0.5 s) per PlayerInfo instance. Server-side thirst
 * mechanics are untouched (this mixin is registered client-only).
 */
@Mixin(targets = "net.dries007.tfc.common.player.PlayerInfo", remap = false)
public abstract class ThirstTemperatureCacheMixin {
    private static final long TFCR_FIXES$CACHE_TICKS = 10L;
    private static Object tfcr_fixes$cachedOwner = null;
    private static long tfcr_fixes$cachedGameTime = Long.MIN_VALUE;
    private static float tfcr_fixes$cachedValue = 0.0F;

    @Inject(method = "getThirstContributionFromTemperature", at = @At("HEAD"), cancellable = true)
    private void tfcr_fixes$useCachedThirstContribution(CallbackInfoReturnable<Float> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (tfcr_fixes$cachedOwner == this && Math.abs(now - tfcr_fixes$cachedGameTime) < TFCR_FIXES$CACHE_TICKS) {
            cir.setReturnValue(tfcr_fixes$cachedValue);
        }
    }

    @Inject(method = "getThirstContributionFromTemperature", at = @At("RETURN"))
    private void tfcr_fixes$storeThirstContribution(CallbackInfoReturnable<Float> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        tfcr_fixes$cachedOwner = this;
        tfcr_fixes$cachedGameTime = level.getGameTime();
        tfcr_fixes$cachedValue = cir.getReturnValue();
    }
}
