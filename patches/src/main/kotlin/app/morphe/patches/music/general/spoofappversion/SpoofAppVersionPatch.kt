/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
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

package app.morphe.patches.music.general.spoofappversion

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC_SPOOF_APP_VERSION
import app.morphe.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.patch.PatchList.SPOOF_APP_VERSION
import app.morphe.patches.music.utils.playservice.is_6_36_or_greater
import app.morphe.patches.music.utils.playservice.is_6_43_or_greater
import app.morphe.patches.music.utils.playservice.is_7_25_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addListPreference
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.morphe.patches.shared.spoof.watchnext.spoofAppVersionWatchNextPatch
import app.morphe.util.Utils.printWarn
import app.morphe.util.appendAppVersion
import app.morphe.util.findMethodOrThrow
import app.morphe.util.returnEarly

private val spoofAppVersionWatchNextPatch = spoofAppVersionWatchNextPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            versionCheckPatch
        )
    },
    patchRequired = {
        is_7_25_or_greater
    },
    availabilityDescriptor = "$GENERAL_CLASS_DESCRIPTOR->spoofWatchNextEndpointAppVersionEnabled()Z",
    appVersionDescriptor = "$GENERAL_CLASS_DESCRIPTOR->getWatchNextEndpointVersionOverride()Ljava/lang/String;"
)

private val spoofAppVersionBytecodePatch = bytecodePatch(
    description = "spoofAppVersionBytecodePatch"
) {
    dependsOn(
        baseSpoofAppVersionPatch("$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"),
        versionCheckPatch,
    )

    execute {
        if (!is_6_36_or_greater) {
            return@execute
        }

        val defaultVersionString = if (is_7_25_or_greater)
            "6.42.55" else "6.35.52"

        findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
            name == "SpoofAppVersionDefaultString"
        }.returnEarly(defaultVersionString)
    }
}

@Suppress("unused")
val spoofAppVersionPatch = resourcePatch(
    // SPOOF_APP_VERSION.title,
    // SPOOF_APP_VERSION.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC_SPOOF_APP_VERSION)

    dependsOn(
        spoofAppVersionBytecodePatch,
        spoofAppVersionWatchNextPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        if (!is_6_36_or_greater) {
            printWarn("\"${SPOOF_APP_VERSION.title}\" is not supported in this version. Use YouTube Music 6.36.54 or later.")
            return@execute
        }
        if (is_7_25_or_greater) {
            appendAppVersion("7.17.52")
        }
        if (is_6_43_or_greater) {
            appendAppVersion("6.42.55")
        }
        if (!is_7_25_or_greater) {
            appendAppVersion("6.35.52")
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_spoof_app_version",
            "false"
        )
        addListPreference(
            CategoryType.GENERAL,
            "revanced_spoof_app_version_target",
            "revanced_spoof_app_version"
        )

        updatePatchStatus(SPOOF_APP_VERSION)

    }
}
