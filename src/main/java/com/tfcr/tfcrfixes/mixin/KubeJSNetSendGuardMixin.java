package com.tfcr.tfcrfixes.mixin;

import dev.latvian.mods.kubejs.net.KubeJSNet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** KubeJSNet.safeSendToPlayer/sendToAllPlayers call NeoForge's PacketDistributor
 *  without any guard. NeoForge's NetworkRegistry.checkPacket throws
 *  UnsupportedOperationException when the target connection never negotiated the
 *  payload's channel - which is exactly what happens on fake connections created
 *  by replay mods (Flashback's ReplayServer "Replay Viewer"), kicking the viewer
 *  and showing "connection lost". We replicate the (tiny) original logic behind a
 *  try/catch so a refused payload is logged once and dropped instead of crashing
 *  the player-join pipeline. Real player connections are unaffected: they never
 *  throw here. */
@Mixin(KubeJSNet.class)
public abstract class KubeJSNetSendGuardMixin {
    private static final Logger TFCR_FIXES$LOGGER = LoggerFactory.getLogger("tfcr_fixes/KubeJSNetSendGuard");
    private static final Set<String> TFCR_FIXES$LOGGED = ConcurrentHashMap.newKeySet();

    @Inject(method = "safeSendToPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tfcr_fixes$guardedSendToPlayer(ServerPlayer player, CustomPacketPayload payload,
                                                       CustomPacketPayload[] payloads, CallbackInfo ci) {
        ci.cancel();
        try {
            PacketDistributorAccess.sendToPlayer(player, payload, payloads);
        } catch (UnsupportedOperationException e) {
            tfcr_fixes$logOnce(payload, e);
        }
    }

    @Inject(method = "sendToAllPlayers", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tfcr_fixes$guardedSendToAll(CustomPacketPayload payload, CustomPacketPayload[] payloads,
                                                    CallbackInfo ci) {
        ci.cancel();
        try {
            PacketDistributorAccess.sendToAllPlayers(payload, payloads);
        } catch (UnsupportedOperationException e) {
            tfcr_fixes$logOnce(payload, e);
        }
    }

    private static void tfcr_fixes$logOnce(CustomPacketPayload payload, UnsupportedOperationException e) {
        String id = String.valueOf(payload.type().id());
        if (TFCR_FIXES$LOGGED.add(id)) {
            TFCR_FIXES$LOGGER.warn("Suppressed unnegotiated payload send '{}' ({}); dropping it instead of crashing the connection",
                id, e.getMessage());
        }
    }

    /** Indirection so the guarded methods above don't inline a hard reference that
     *  would also need the serverOnly check; kept package-visible for the mixin only. */
    private static final class PacketDistributorAccess {
        static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
            if (dev.latvian.mods.kubejs.CommonProperties.get().serverOnly) {
                return;
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload, payloads);
        }

        static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads) {
            if (dev.latvian.mods.kubejs.CommonProperties.get().serverOnly) {
                return;
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(payload, payloads);
        }
    }
}
