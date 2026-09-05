package com.tfcr.tfcrfixes.mixin;

import java.util.Iterator;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
 *  "Can't find id for Reference{...banner_pattern/rhombus...}" and kills the
 *  replay session.
 *
 *  Note the viewer check cannot rely on player.connection alone:
 *  FlashbackFakePlayer (the recorded players re-spawned into the replay world)
 *  has NO connection at all, and during placeNewPlayer the login event can
 *  fire before the connection field is assigned. So we treat every player on a
 *  Flashback ReplayServer as a replay-side player - nobody there is actually
 *  playing, so no architectury server->player sync is ever meaningful. */
@Mixin(NetworkManager.class)
public abstract class ArchitecturyReplaySendSkipMixin {
    private static final Logger TFCR_FIXES$LOGGER = LoggerFactory.getLogger("tfcr_fixes/ReplayPacketGuard");
    private static volatile boolean tfcr_fixes$logged = false;

    private static boolean tfcr_fixes$isReplaySide(ServerPlayer player) {
        boolean replay = player.connection != null
                && player.connection.getClass().getName().toLowerCase().contains("flashback");
        if (!replay) {
            MinecraftServer server = player.getServer();
            replay = server != null && server.getClass().getName().toLowerCase().contains("flashback");
        }
        if (replay && !tfcr_fixes$logged) {
            tfcr_fixes$logged = true;
            TFCR_FIXES$LOGGER.info("Flashback replay detected; skipping architectury server->player sync packets on it");
        }
        return replay;
    }

    private static boolean tfcr_fixes$isReplaySide(Iterable<ServerPlayer> players) {
        Iterator<ServerPlayer> it = players.iterator();
        // A Flashback ReplayServer only ever hosts replay-side players, so
        // sampling the first one is enough.
        return it.hasNext() && tfcr_fixes$isReplaySide(it.next());
    }

    @Inject(method = "sendToPlayer(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/RegistryFriendlyByteBuf;)V",
            at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$skipBufSendForReplayViewer(ServerPlayer player, ResourceLocation id, RegistryFriendlyByteBuf buf, CallbackInfo ci) {
        if (tfcr_fixes$isReplaySide(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendToPlayer(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
            at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$skipPayloadSendForReplayViewer(ServerPlayer player, CustomPacketPayload payload, CallbackInfo ci) {
        if (tfcr_fixes$isReplaySide(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendToPlayers(Ljava/lang/Iterable;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/RegistryFriendlyByteBuf;)V",
            at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$skipBufBroadcastForReplay(Iterable<ServerPlayer> players, ResourceLocation id, RegistryFriendlyByteBuf buf, CallbackInfo ci) {
        if (tfcr_fixes$isReplaySide(players)) {
            ci.cancel();
        }
    }

    @Inject(method = "sendToPlayers(Ljava/lang/Iterable;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V",
            at = @At("HEAD"), cancellable = true)
    private static void tfcr_fixes$skipPayloadBroadcastForReplay(Iterable<ServerPlayer> players, CustomPacketPayload payload, CallbackInfo ci) {
        if (tfcr_fixes$isReplaySide(players)) {
            ci.cancel();
        }
    }
}
