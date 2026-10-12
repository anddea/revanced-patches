/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project.
 *
 * Original author(s):
 * - lavinhoque33 (https://github.com/lavinhoque33)
 *
 * Licensed under the GNU General Public License v3.0.
 */

package app.morphe.patches.youtube.utils.bottomsheet

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.insertLiteralOverrideAtInlinedSites

/**
 * Server flag that shows 3-dots menus as a popup anchored to the button (YouTube 21.3x+).
 * The custom flyout items (Save to Watch later for Shorts and kids videos, download items,
 * Shorts custom actions) are added to the bottom sheet menu, so keep that menu.
 */
private const val ANCHORED_FLYOUT_MENU_FEATURE_FLAG = 45748489L

internal val bottomSheetFlyoutMenuPatch = bytecodePatch(
    description = "bottomSheetFlyoutMenuPatch"
) {
    execute {
        // Versions without the flag have nothing to override.
        insertLiteralOverrideAtInlinedSites(ANCHORED_FLYOUT_MENU_FEATURE_FLAG, false)
    }
}
