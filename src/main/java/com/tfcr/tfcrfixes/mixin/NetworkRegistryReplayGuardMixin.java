package com.tfcr.tfcrfixes.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NeoForge's NetworkRegistry.checkPacket throws UnsupportedOperationException
 *  when a modded payload is sent to a connection that never negotiated its
 *  channel. Replay mods (Flashback) drive a "Replay Viewer" through a fake
 *  connection, so EVERY mod's datapack-sync packet (kubejs, l2core, TFC,
 *  sophisticatedcore, sable, ...) detonates placeNewPlayer in turn and the
 *  viewer gets kicked with "connection lost". The fake connection just queues
 *  packets locally, so the check is pure bookkeeping overhead there - skip it
 *  for flashback listeners and let the payloads flow (the viewing client has
 *  the mods installed and can decode them). */
@Mixin(NetworkRegistry.class)
public abstract class NetworkRegistryReplayGuardMixin {
    private static final Logger TFCR_FIXES$LOGGER = LoggerFactory.getLogger("tfcr_fixes/ReplayPacketGuard");
    private static volatile boolean tfcr_fixes$logged = false;

    @Inject(method = "checkPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/protocol/common/ServerCommonPacketListener;)V",
            at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$skipCheckForReplayViewer(Packet<?> packet, ServerCommonPacketListener listener, CallbackInfo ci) {
        if (listener.getClass().getName().contains("flashback")) {
            if (!tfcr_fixes$logged) {
                tfcr_fixes$logged = true;
                TFCR_FIXES$LOGGER.info("Replay viewer detected ({}); skipping NeoForge payload channel checks for it",
                    listener.getClass().getSimpleName());
            }
            ci.cancel();
        }
    }
}
