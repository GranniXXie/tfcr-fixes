package com.tfcr.tfcrfixes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Defensive fixes for the TerraFirma Rebirth pack.
 *
 * Client mixins: guards DynamicTexture uploads against null pixels.
 * Common mixins: stops tfc_coldsweat's climate modifier from feeding garbage
 * temperatures in Cosmonautics dimensions (moon / deep space); guards Create
 * contraption collision against a null separation manifold (server crash);
 * guards sable seat position corruption.
 *
 * Additionally blacklists the Aeronautics contraption diagram screen from
 * FancyMenu's screen customization: FancyMenu's render wrapper reorders the
 * screen's custom framebuffer drawing, which makes the diagram's data columns
 * flicker (upstream issue Simulated-Project#46).
 *
 * NOTE: the TFC chest contraption dupe is fixed pack-side via the
 * create:chest_mounted_storage block tag in kubejs (tags/block.js) — the
 * code-side registration attempts were retired because Create's mounted
 * storage association is tag-driven and only resolvable after tags load.
 */
@Mod(value = TFCRFixes.MODID)
public class TFCRFixes {
    public static final String MODID = "tfcr_fixes";

    public TFCRFixes(IEventBus modEventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("fancymenu")) {
            FancyMenuCompat.blacklistDiagramScreen();
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.tfcr.tfcrfixes.client.RecipeButtonClickHandler.register();
        }
    }
}
