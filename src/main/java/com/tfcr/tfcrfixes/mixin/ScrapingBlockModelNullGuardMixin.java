package com.tfcr.tfcrfixes.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.client.model.ScrapingBlockModel;
import net.dries007.tfc.common.blockentities.ScrapingBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TFC's ScrapingBlockModel.render() reads ScrapingBlockEntity#getInputTexture /
 *  getOutputTexture twice: once for the null check, once as the atlas lookup key.
 *  Sodium builds chunk meshes on worker threads while the main thread can clear
 *  those fields via updateDisplayCache() (item insert/remove/scrape), so the
 *  second read can return null and TextureAtlas.getSprite(null) NPEs, killing the
 *  client. We take over render() at HEAD and capture both textures exactly once,
 *  so a concurrent clear simply yields the missing texture instead of a crash. */
@Mixin(ScrapingBlockModel.class)
public abstract class ScrapingBlockModelNullGuardMixin {
    @Invoker(value = "drawTiles", remap = false)
    abstract void tfcr_fixes$drawTiles(VertexConsumer buffer, PoseStack poseStack, ResourceLocation texture,
                                       short positions, int condition, int packedLight, int packedOverlay, int color);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void tfcr_fixes$atomicRender(ScrapingBlockEntity scraping, PoseStack poseStack, VertexConsumer buffer,
                                         int packedLight, int packedOverlay, CallbackInfoReturnable<TextureAtlasSprite> cir) {
        // Capture once: these fields can be nulled mid-frame by the main thread.
        ResourceLocation in = scraping.getInputTexture();
        ResourceLocation out = scraping.getOutputTexture();
        if (in != null && out != null) {
            short positions = scraping.getScrapedPositions();
            tfcr_fixes$drawTiles(buffer, poseStack, in, positions, 0, packedLight, packedOverlay, scraping.getColor1());
            tfcr_fixes$drawTiles(buffer, poseStack, out, positions, 1, packedLight, packedOverlay, scraping.getColor2());
        }
        if (out != null) {
            cir.setReturnValue(Minecraft.getInstance().getTextureAtlas(RenderHelpers.BLOCKS_ATLAS).apply(out));
        } else {
            cir.setReturnValue(RenderHelpers.missingTexture());
        }
    }
}
