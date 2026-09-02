package com.tfcr.tfcrfixes.mixin;

import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * The pack ships a fml.toml dependency override (`dependencyOverrides.sable =
 * ["-scalablelux"]`) so Sable loads without ScalableLux. NeoForge answers this
 * with a blocking startup warning screen: "Unknown mod scalablelux referenced in
 * dependency overrides for mod sable". The warning is pure noise for players —
 * we deliberately removed ScalableLux — so we filter exactly that issue out of
 * the list that decides whether the warning screen opens. It still gets logged.
 */
@Mixin(targets = "net.neoforged.neoforge.client.loading.ClientModLoader", remap = false)
public abstract class ClientModLoaderWarningFilterMixin {
    @Redirect(method = "completeModLoading",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/ModLoader;getLoadingIssues()Ljava/util/List;"))
    private static List<ModLoadingIssue> tfcr_fixes$filterDependencyOverrideNoise() {
        return ModLoader.getLoadingIssues().stream()
                .filter(issue -> !("fml.modloadingissue.depoverride.unknown_dependency".equals(issue.translationKey())
                        && issue.translationArgs().contains("scalablelux")))
                .toList();
    }
}
