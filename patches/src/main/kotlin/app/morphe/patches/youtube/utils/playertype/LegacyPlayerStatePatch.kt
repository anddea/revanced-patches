/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.playertype

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playservice.is_20_29_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_04_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_36_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.insertLiteralOverride

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerControlsPatch;"

private object NewPlayerTypeEnumFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45698813)
    )
)

private object NewPlayerOverlaysFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45752335L)
    )
)

internal val legacyPlayerStatePatch = bytecodePatch(
    description = "Keep legacy player state callbacks available."
) {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    execute {
        if (is_21_36_or_greater) {
            NewPlayerTypeEnumFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->forcePlayerSeekbar(Z)Z",
                )
            }
        } else if (is_20_29_or_greater) {
            // Client flag that interferes with background playback of some video types.
            // Exact purpose is not clear and it's used in ~ 100 locations.
            // Flag cannot be forced off with 21.36+ or the player seekbar is missing.
            //
            // Edit: This override may not be needed and only 45752335L override might be needed.
            NewPlayerTypeEnumFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(it.instructionMatches.first().index, false)
            }
        }

        // If NewPlayerTypeEnumFeatureFlagFingerprint is overridden then must also
        // force off new player control flags otherwise player has no buttons visible.
        if (is_21_04_or_greater) {
            NewPlayerOverlaysFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    false
                )
            }
        }
    }
}
