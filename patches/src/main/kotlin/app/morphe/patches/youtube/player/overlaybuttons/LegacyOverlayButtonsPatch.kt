/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/954
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.overlaybuttons

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playercontrols.playerControlsPatch
import app.morphe.patches.youtube.utils.playservice.is_21_08_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_11_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_13_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_15_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_36_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.insertLiteralOverride

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/PlayerControlsPatch;"

/** Feature flag used by the newer top-control layout. */
private object ModernPlayerTopControlsFeatureFlagFingerprint : Fingerprint(
    filters = listOf(literal(45750838L))
)

/**
 * Elevated player, which shows the video metadata in a player overlay.
 */
private object ElevatedPlayerFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45757309)
    )
)

private object PlayerCommentTeaserFeatureFlagFingerprint : Fingerprint(
    filters = listOf(literal(45771730L))
)

/**
 * Watch feed resize: scrolling the watch next feed resizes the player,
 * and every scroll event relayouts the whole watch page.
 */
private object WatchFeedResizeFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45763727L) // 21.11+
    )
)

private val restoreOldPlayerButtonsSupported
    get() = is_21_13_or_greater && !is_21_36_or_greater

/**
 * Keeps the current overlay-button implementation usable when YouTube's modern player controls
 * are disabled at runtime. The legacy row itself is supplied by [overlayButtonsPatch].
 */
internal val legacyOverlayButtonsPatch = bytecodePatch(
    description = "legacyOverlayButtonsPatch",
) {
    dependsOn(
        playerControlsPatch,
        sharedExtensionPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {
        if (is_21_08_or_greater) {
            // The elevated player shows the video metadata without a background over the
            // ambient mode colors, so the light theme text can not be read.
            ElevatedPlayerFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    false
                )
            }
        }

        // Watch feed resize A/B test: every scroll event of the watch next feed
        // resizes the player and relayouts the watch page, and scrolling lags.
        // It also interferes with the old player icons, so always turn it off.
        if (is_21_11_or_greater) {
            WatchFeedResizeFeatureFlagFingerprint.matchAllMethodIndicesForEach { index ->
                insertLiteralOverride(index, false)
            }
        }

        if (!restoreOldPlayerButtonsSupported) return@execute

        addPreference(arrayOf("SETTINGS: RESTORE_OLD_PLAYER_BUTTONS"))

        ModernPlayerTopControlsFeatureFlagFingerprint.matchAll().forEach {
            it.method.insertLiteralOverride(
                it.instructionMatches.first().index,
                "$EXTENSION_CLASS_DESCRIPTOR->useModernPlayerTopControls(Z)Z",
            )
        }

        if (is_21_15_or_greater) {
            PlayerCommentTeaserFeatureFlagFingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->allowModernPlayerLayoutFlags(Z)Z",
                )
            }
        }
    }
}
