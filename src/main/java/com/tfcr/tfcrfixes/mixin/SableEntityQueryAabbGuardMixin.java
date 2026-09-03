package com.tfcr.tfcrfixes.mixin;

import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;

/**
 * Watchdog-hang guard for Sable's sublevel-aware entity getter
 * (official-server hang, TFCR 4.1.24-hotfix2, 2026-09-03).
 *
 * Incident: the server main thread hung forever inside
 * EntitySectionStorage.getEntities, entered through
 * SubLevelInclusiveLevelEntityGetter.get(AABB, Consumer) at line 68 — the
 * FIRST plain delegate query, before any sublevel transform.
 *
 * Root cause: vanilla EntitySectionStorage.getEntities computes its section
 * iteration bounds with Mth.floor(aabb.min/max). If any bound is NaN or
 * infinite (a "garbage pose" entity — e.g. a duped/ghost item entity with a
 * corrupted position), the loop bounds become +-2^31 and the tick never
 * finishes. Sable's own oversize guard (`getSize() > 100000 -> reject`) is
 * NaN-transparent: every comparison against NaN is false, so a degenerate
 * AABB sails straight through it.
 *
 * Fixes applied here:
 *  1. HEAD guard on both AABB-based get(...) overloads: reject any AABB with
 *     a non-finite coordinate, and enforce the same 100000-size ceiling in a
 *     NaN-proof way, before any work happens.
 *  2. Sanitize the two BoundingBox3d.toMojang() results (forward transform of
 *     the containing sublevel, inverse transform per intersecting sublevel):
 *     if a corrupted sublevel pose turns the query box into garbage, fall
 *     back to the original untransformed AABB instead of feeding infinity
 *     into the section storage.
 *
 * The bad entity itself is left alone — vanilla item despawn / the sweeper
 * removes it; the point is that one rotten entity can never again take the
 * whole server down.
 */
@Mixin(targets = "dev.ryanhcode.sable.util.SubLevelInclusiveLevelEntityGetter", remap = false)
public abstract class SableEntityQueryAabbGuardMixin {

    @Unique
    private static final Logger TFCR_FIXES$LOGGER = LoggerFactory.getLogger("tfcr_fixes/SableAabbGuard");

    /** Matches Sable's own MAX_GET_SIDE_LENGTH guard threshold. */
    @Unique
    private static final double TFCR_FIXES$MAX_SIZE = 100000.0D;

    @Unique
    private static long tfcr_fixes$suppressed = 0L;

    @Unique
    private static boolean tfcr_fixes$isBad(AABB aabb) {
        if (aabb == null) {
            return true;
        }
        if (!Double.isFinite(aabb.minX) || !Double.isFinite(aabb.minY) || !Double.isFinite(aabb.minZ)
                || !Double.isFinite(aabb.maxX) || !Double.isFinite(aabb.maxY) || !Double.isFinite(aabb.maxZ)) {
            return true;
        }
        double size = aabb.getSize();
        return !Double.isFinite(size) || size > TFCR_FIXES$MAX_SIZE;
    }

    @Unique
    private static void tfcr_fixes$log(String where, AABB aabb) {
        if (tfcr_fixes$suppressed < 10L || tfcr_fixes$suppressed % 1000L == 0L) {
            TFCR_FIXES$LOGGER.warn("Rejected degenerate entity-collision query box at {}: {}", where, aabb);
        }
        tfcr_fixes$suppressed++;
    }

    @Inject(method = "get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void tfcr_fixes$guardGet(AABB aabb, Consumer<?> consumer, CallbackInfo ci) {
        if (tfcr_fixes$isBad(aabb)) {
            tfcr_fixes$log("get(AABB)", aabb);
            ci.cancel();
        }
    }

    @Inject(method = "get(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void tfcr_fixes$guardGetTyped(net.minecraft.world.level.entity.EntityTypeTest<?, ?> test, AABB aabb,
                                          net.minecraft.util.AbortableIterationConsumer<?> consumer, CallbackInfo ci) {
        if (tfcr_fixes$isBad(aabb)) {
            tfcr_fixes$log("get(EntityTypeTest, AABB)", aabb);
            ci.cancel();
        }
    }

    /**
     * Sanitize the forward-transformed box (containing sublevel) and every
     * inverse-transformed box (intersecting sublevels) in get(AABB, Consumer).
     * The handler covers both toMojang() call sites in that method.
     */
    @Redirect(method = "get(Lnet/minecraft/world/phys/AABB;Ljava/util/function/Consumer;)V",
            at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/companion/math/BoundingBox3d;toMojang()Lnet/minecraft/world/phys/AABB;"),
            remap = false)
    private AABB tfcr_fixes$sanitizeToMojang(BoundingBox3d boundingBox, AABB originalAabb, Consumer<?> consumer) {
        AABB transformed = boundingBox.toMojang();
        if (tfcr_fixes$isBad(transformed)) {
            tfcr_fixes$log("get(AABB) sublevel transform", transformed);
            return originalAabb;
        }
        return transformed;
    }

    /**
     * Same sanitation for the EntityTypeTest overload.
     */
    @Redirect(method = "get(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/util/AbortableIterationConsumer;)V",
            at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/companion/math/BoundingBox3d;toMojang()Lnet/minecraft/world/phys/AABB;"),
            remap = false)
    private AABB tfcr_fixes$sanitizeToMojangTyped(BoundingBox3d boundingBox, net.minecraft.world.level.entity.EntityTypeTest<?, ?> test,
                                                  AABB originalAabb,
                                                  net.minecraft.util.AbortableIterationConsumer<?> consumer) {
        AABB transformed = boundingBox.toMojang();
        if (tfcr_fixes$isBad(transformed)) {
            tfcr_fixes$log("get(EntityTypeTest, AABB) sublevel transform", transformed);
            return originalAabb;
        }
        return transformed;
    }
}
