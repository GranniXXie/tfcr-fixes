package com.tfcr.tfcrfixes.mixin;

import dev.latvian.mods.kubejs.player.KubeJSPlayerEventHandler;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flashback's ReplayServer spawns a "Replay Viewer" player over a fake
 *  connection. KubeJS's datapackSync fires during placeNewPlayer and tries to
 *  send kubejs:sync_server_data to it; NeoForge refuses the unnegotiated payload
 *  and the exception aborts player spawn, showing the viewer "connection lost".
 *  The replay viewer has no use for KubeJS server data, so skip the sync entirely
 *  when the server is a replay server. (The generic send guard in
 *  KubeJSNetSendGuardMixin covers any other mod taking the same path.) */
@Mixin(KubeJSPlayerEventHandler.class)
public abstract class KubeJSReplayDatapackSyncMixin {
    @Inject(method = "datapackSync", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tfcr_fixes$skipReplayServerSync(OnDatapackSyncEvent event, CallbackInfo ci) {
        if (event.getPlayerList().getServer().getClass().getName().contains("flashback")) {
            ci.cancel();
        }
    }
}
