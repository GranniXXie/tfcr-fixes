package com.tfcr.tfcrfixes;

import de.keksuccino.fancymenu.customization.ScreenCustomization;

/**
 * Isolated so the FancyMenu classes only get loaded when FancyMenu is present
 * (guarded by ModList check in TFCRFixes).
 */
public final class FancyMenuCompat {

    private static final String DIAGRAM_SCREEN =
            "dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen";

    private FancyMenuCompat() {
    }

    public static void blacklistDiagramScreen() {
        ScreenCustomization.addScreenBlacklistRule(DIAGRAM_SCREEN::equals);
    }
}
