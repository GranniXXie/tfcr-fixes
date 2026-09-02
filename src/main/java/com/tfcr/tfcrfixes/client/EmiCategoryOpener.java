package com.tfcr.tfcrfixes.client;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.registry.EmiRecipes;
import net.minecraft.resources.ResourceLocation;

/**
 * TFC draws "?" recipe buttons on BarrelScreen / PotScreen whenever JEI is loaded,
 * and wires them through its JEI plugin. But EMI's JEMI bridge skips every phase of
 * a mod's JEI plugin once that mod ships its own EMI plugin, so TFC's click areas
 * are never registered and the buttons are dead. This helper re-routes those clicks
 * into EMI, which is the recipe UI this pack actually shows.
 */
public final class EmiCategoryOpener {
    private EmiCategoryOpener() {}

    public static void open(String namespace, String path) {
        try {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
            for (EmiRecipeCategory category : EmiRecipes.categories) {
                if (category.getId().equals(id)) {
                    EmiApi.displayRecipeCategory(category);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // EMI runtime not ready yet or category missing; leave the click unhandled.
        }
    }
}
