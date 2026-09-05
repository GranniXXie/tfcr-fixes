package com.tfcr.tfcrfixes.mixin;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Entity data accessor ids shift whenever a mod adds/removes a synced field,
 *  so a Flashback recording made with a different pack build carries
 *  SetEntityData values whose serializer no longer matches the current entity
 *  class (e.g. TFCPlowEntity field 14: recorded ItemStack vs current Boolean).
 *  Vanilla assignValues() throws IllegalStateException on the first mismatch
 *  and takes down the whole replay. A replay is only a VIEWING session, so on
 *  Flashback's ReplayServer we drop just the mismatched values and keep going
 *  (also guards out-of-range ids, which would AIOOBE the itemsById array).
 *  Normal gameplay is untouched - there a mismatch still throws, as it should. */
@Mixin(SynchedEntityData.class)
public abstract class ReplayEntityDataMismatchGuardMixin {
    private static final Logger TFCR_FIXES$LOGGER = LoggerFactory.getLogger("tfcr_fixes/ReplayEntityDataGuard");
    private static final Set<String> TFCR_FIXES$LOGGED = new HashSet<>();
    private static final int TFCR_FIXES$MAX_LOGS = 20;

    @Shadow @Final private SynchedEntityData.DataItem<?>[] itemsById;
    @Shadow @Final private SyncedDataHolder entity;

    @Inject(method = "assignValues", at = @At("HEAD"), cancellable = true)
    private void tfcr_fixes$dropMismatchedReplayValues(List<SynchedEntityData.DataValue<?>> values, CallbackInfo ci) {
        if (!(this.entity instanceof Entity entity)) return;
        MinecraftServer server = entity.level().getServer();
        if (server == null || !server.getClass().getName().toLowerCase().contains("flashback")) return;

        Set<SynchedEntityData.DataValue<?>> bad = null;
        for (SynchedEntityData.DataValue<?> v : values) {
            int id = v.id();
            SynchedEntityData.DataItem<?> item = id >= 0 && id < this.itemsById.length ? this.itemsById[id] : null;
            if (item == null || !Objects.equals(v.serializer(), item.getAccessor().serializer())) {
                if (bad == null) bad = Collections.newSetFromMap(new IdentityHashMap<>());
                bad.add(v);
                if (TFCR_FIXES$LOGGED.size() < TFCR_FIXES$MAX_LOGS
                        && TFCR_FIXES$LOGGED.add(entity.getType() + "#" + id)) {
                    TFCR_FIXES$LOGGER.warn("Dropping mismatched entity data field {} on {} (recorded {} vs current {})",
                        id, entity,
                        item == null ? "<no such field>" : String.valueOf(v.value()),
                        item == null ? "-" : String.valueOf(item.getValue()));
                }
            }
        }
        if (bad == null) return;

        // Replay vanilla assignValues() minus the bad entries. Done manually
        // (instead of removeIf) because the packet's list may be immutable.
        for (SynchedEntityData.DataValue<?> v : values) {
            if (bad.contains(v)) continue;
            SynchedEntityData.DataItem<?> item = this.itemsById[v.id()];
            tfcr_fixes$assignRaw(item, v.value());
            this.entity.onSyncedDataUpdated(item.getAccessor());
        }
        this.entity.onSyncedDataUpdated(values);
        ci.cancel();
    }

    @SuppressWarnings("unchecked")
    private static void tfcr_fixes$assignRaw(SynchedEntityData.DataItem<?> item, Object value) {
        ((SynchedEntityData.DataItem<Object>) item).setValue(value);
    }
}
