package com.tfcr.tfcrfixes.client;

import net.dries007.tfc.client.screen.BarrelScreen;
import net.dries007.tfc.client.screen.PotScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * TFC's JEI plugin registers "?" recipe click areas on PotScreen (77,6 9x14) and
 * BarrelScreen (92,21 9x14). In this pack EMI is the recipe UI and the click areas
 * end up dead. Re-route the clicks to the matching EMI category via the screen
 * event bus — no mixin, immune to TFC method-name changes.
 */
public final class RecipeButtonClickHandler {
    private RecipeButtonClickHandler() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RecipeButtonClickHandler::onMousePressed);
    }

    private static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0) {
            return;
        }
        Screen screen = event.getScreen();
        String category = null;
        if (screen instanceof PotScreen && inside(event, 77, 6, 9, 14)) {
            category = "pot";
        } else if (screen instanceof BarrelScreen && inside(event, 92, 21, 9, 14)) {
            category = "barrel";
        }
        if (category != null) {
            EmiCategoryOpener.open("tfc", category);
            event.setCanceled(true);
        }
    }

    private static boolean inside(ScreenEvent.MouseButtonPressed.Pre event, int x, int y, int w, int h) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        double relX = event.getMouseX() - screen.getGuiLeft();
        double relY = event.getMouseY() - screen.getGuiTop();
        return relX >= x && relX < x + w && relY >= y && relY < y + h;
    }
}
