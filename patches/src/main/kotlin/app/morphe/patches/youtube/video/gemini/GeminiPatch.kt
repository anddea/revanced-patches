/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.youtube.video.gemini

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.player.overlaybuttons.overlayButtonsPatch
import app.morphe.patches.youtube.shorts.components.shortsComponentPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.GEMINI
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.updatePatchStatus

private val geminiBytecodePatch = bytecodePatch(
    description = "geminiBytecodePatch"
) {
    dependsOn(
        overlayButtonsPatch,
        settingsPatch,
        shortsComponentPatch,
        sharedExtensionPatch,
    )

    execute {
        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "Gemini")
    }
}

/**
 * Owns all user-facing Gemini controls while reusing the generic player-overlay and Shorts
 * custom-action infrastructure.
 */
@Suppress("unused")
val geminiPatch = resourcePatch(
    GEMINI.title,
    GEMINI.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        geminiBytecodePatch,
        overlayButtonsPatch,
        settingsPatch,
        shortsComponentPatch,
    )

    execute {
        addPreference(
            arrayOf("PREFERENCE_SCREEN: GEMINI"),
            GEMINI
        )
    }
}
