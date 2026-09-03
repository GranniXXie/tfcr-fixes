package com.tfcr.tfcrfixes.client;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.IFood;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * EMI builds its search index from creative-tab stacks at startup. TFC attaches a
 * food component with the current calendar date to those stacks, so after enough
 * in-game time every indexed TFC food turns rotten — the tooltip then hides the
 * nutrition values players are searching for.
 *
 * TFC itself uses creation date flag -2 (fresh, never decays) for the food stacks
 * it hands to its own EMI recipe displays. This plugin swaps every dated food
 * stack in the EMI index for its -2 equivalent. Stacks already carrying a display
 * flag (-1/-2/-3/-4) are left untouched. Recipe ingredients are not part of the
 * index and are unaffected.
 *
 * Additionally hides registry junk that has no recipes by design:
 *  - TFCAstikorCarts registers carts/wheels for 38 AFC "leaf-only" wood species
 *    (acacia_koa etc.) that have no log/planks blocks at all, so these 266 items
 *    can never be crafted.
 *  - woodencog's "unfinished" intermediate items (missing models/textures, show
 *    as purple-black cubes).
 */
@EmiEntrypoint
public class TfcrEmiPlugin implements EmiPlugin {

    private static final long NON_DECAY_FLAG = -2L;

    /** AFC species that only register leaves/saplings — no log, no planks, no recipes. */
    private static final Set<String> LEAF_ONLY_WOODS = Set.of(
            "acacia_koa", "atlas_cedar", "bald_cypress", "balsam_fir", "bigleaf_maple",
            "black_beech", "black_spruce", "chinquapin", "coast_redwood", "coast_spruce",
            "columnar_araucaria", "dawn_redwood", "flame_of_the_forest", "giant_rosewood",
            "hardy_chestnut", "horsetail_ironwood", "huangshan_pine", "iroko_teak",
            "jaggery_palm", "juniper", "lebombo_ironwood", "live_oak", "mountain_ash",
            "mountain_fir", "mpingo_blackwood", "parana", "rauli_beech", "red_pine",
            "red_silk_cotton", "sapele_mahogany", "scrub_hickory", "sitka_spruce",
            "small_leaf_mahogany", "stone_pine", "tamarack", "weeping_cypress",
            "weeping_maple", "weeping_willow"
    );

    private static final Pattern CART_ITEM = Pattern.compile(
            "^(wheel|animal_cart|hand_cart|plow|reaper|seed_drill|supply_cart)/(.+)$");
    private static final Pattern WOODENCOG_UNFINISHED = Pattern.compile(".*/unfinished$");

    @Override
    public void register(EmiRegistry registry) {
        // Remove junk stacks and every dated (decaying) food stack from the index.
        // NOTE: removeEmiStacks() only stores the predicate — EMI defers evaluation
        // to EmiStackList.bake(), which runs AFTER all plugins' register() calls.
        // Collecting replacement stacks from inside the predicate therefore always
        // yields an empty list at addEmiStack() time. That was the bug that left
        // rotten food in the index with no fresh replacement.
        registry.removeEmiStacks(stack -> {
            ItemStack itemStack = stack.getItemStack();
            if (itemStack.isEmpty()) {
                return false;
            }
            if (isHiddenJunk(itemStack)) {
                return true;
            }
            IFood food = FoodCapability.get(itemStack);
            if (food == null) {
                return false;
            }
            long date = food.getCreationDate();
            // Dated stacks (>= 0) rot over time in the index; stacks already
            // sanitized to ROTTEN_FLAG (-4) are rotten forever. Both must go —
            // the -2 never-decay variants added below are their replacements.
            // -1 (transient, TFC recipe outputs) and -3 (visible never-decay)
            // display as fresh and stay.
            return date >= 0 || date == -4L;
        });

        // Add the never-decaying (-2) variant of every food item straight from the
        // item registry, independent of the deferred removal above. The -2 flag is
        // TFC's "invisible never-decay" marker (no tooltip), the same one TFC uses
        // for its own EMI recipe displays, so nutrition tooltips stay readable.
        // Items whose default stack already carries a flag (< 0) are skipped.
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack def = item.getDefaultInstance();
            if (def.isEmpty()) {
                continue;
            }
            IFood food = FoodCapability.get(def);
            if (food == null || food.getCreationDate() < 0) {
                continue;
            }
            registry.addEmiStack(EmiStack.of(FoodCapability.setCreationDate(def.copy(), NON_DECAY_FLAG)));
        }
    }

    private static boolean isHiddenJunk(ItemStack stack) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (id.startsWith("woodencog:")) {
            return WOODENCOG_UNFINISHED.matcher(id.substring("woodencog:".length())).matches();
        }
        if (id.startsWith("tfcastikorcarts:")) {
            var m = CART_ITEM.matcher(id.substring("tfcastikorcarts:".length()));
            return m.matches() && LEAF_ONLY_WOODS.contains(m.group(2));
        }
        return false;
    }
}
