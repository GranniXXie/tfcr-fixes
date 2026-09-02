package com.tfcr.tfcrfixes.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** DynamicTexture queues its GPU upload when constructed off the render thread.
 *  If the backing NativeImage is closed or never set before the queue replays
 *  (map mods building textures on background threads hit this), the upload
 *  dereferences null pixels and kills the render thread. Skip the upload instead:
 *  the texture simply stays blank until its owner uploads real pixels. */
@Mixin(DynamicTexture.class)
public abstract class DynamicTextureNullGuardMixin {
    @Shadow
    private NativeImage pixels;

    @Inject(method = "lambda$new$0", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfcr_fixes$guardConstructorUpload(CallbackInfo ci) {
        if (this.pixels == null) {
            ci.cancel();
        }
    }

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfcr_fixes$guardUpload(CallbackInfo ci) {
        if (this.pixels == null) {
            ci.cancel();
        }
    }
}
