package com.tfcr.tfcrfixes.mixin;

import net.dries007.tfc.common.blocks.IForgeBlockExtension;
import net.dries007.tfc.common.blocks.wood.BookshelfBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TFC 的 IForgeBlockExtension 覆盖了 IBlockExtension#getEnchantPowerBonus，
 * 转发到 ExtendedProperties.enchantmentPowerGetter（默认恒 0），而书架从未设置过它——
 * 所以 enchantment_power_provider 标签和 IBlockExtension 层 mixin 都对 TFC 方块无效。
 * 这里在更具体的 IForgeBlockExtension 层兜底：书架按 TFC 自带的槽位算法给加成
 * （每放一本书 +0.5，满架 6 本 = 3.0，5 个满书架即可满级附魔）。
 */
@Mixin(value = IForgeBlockExtension.class, remap = false)
public interface TfcBookshelfEnchantPowerMixin {

    @Inject(method = "getEnchantPowerBonus", at = @At("RETURN"), cancellable = true, remap = false)
    default void tfcr_fixes$tfcBookshelfPower(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (cir.getReturnValue() <= 0.0f && this instanceof BookshelfBlock) {
            cir.setReturnValue((float) BookshelfBlock.getEnchantPower(state));
        }
    }
}
