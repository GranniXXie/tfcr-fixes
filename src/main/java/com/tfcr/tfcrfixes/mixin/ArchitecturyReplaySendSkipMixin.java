package com.tfcr.tfcrfixes.mixin;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.architectury.networking.NetworkManager;

/** FTB Library syncs its "known server registries" snapshot (a pile of
 *  ItemStacks) to every player on join through architectury networking. Inside
 *  a Flashback replay the registry instances differ from the ones those stacks
 *  were built against, so encoding detonates placeNewPlayer with
 *  "Can't find id for Reference{...banner_pattern/rhombus...}" and the replay
 *  viewer gets kicked. A replay viewer is only WATCHING - no mod needs to
 *  sync anything to it - so skip architectury server->player sends for
 *  Flashback's fake listener entirely (also shields every other
 *  architectury-based mod's join sync). */
@Mixin(NetworkManager.class)
public abstract class ArchitecturyReplaySendSkipMixin {
    private static final Logger TFCR_FIXES$LOGGER = LoggerFactory.getLogger("tfcr_fixes/ReplayPacketGuard");
    private static volatile boolean tfcr_fixes$logged = false;

    private static boolean tfcr_fixes$isReplayViewer(ServerPlayer player) {
        if (player.connection == null) return false;
        if (!player.connection.getClass().getName().toLowerCase().contains("flashback")) return false;
        if (!tfcr_fixes$logged) {
            tfcr_fixes$logged = true;
            TFCR_FIXES$LOGGER.info("Replay viewer detected; skipping architectury server->player sync packets for it");
        }
        return true;
    }

    @Inject(method = "sendToPlayer(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/RegistryFriendlyByteBuf;)V",
            at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$skipBufSendForReplayViewer(ServerPlayer player, ResourceLocation id, RegistryFriendlyByteBuf buf, CallbackInfo ci) {
        if (tfcr_fixes$isReplayViewer(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendToPlayer(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
            at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$skipPayloadSendForReplayViewer(ServerPlayer player, CustomPacketPayload payload, CallbackInfo ci) {
        if (tfcr_fixes$isReplayViewer(player)) {
            ci.cancel();
        }
    }
}
