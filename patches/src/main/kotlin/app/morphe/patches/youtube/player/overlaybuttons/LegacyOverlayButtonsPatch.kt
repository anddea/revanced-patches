/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.overlaybuttons

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playercontrols.playerControlsPatch
import app.morphe.patches.youtube.utils.playservice.is_21_13_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_15_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_36_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.insertLiteralOverride

private const val EXTENSION_CLASS_DESCRIPTOR = "$UTILS_PATH/PlayerControlsPatch;"

/** Feature flag used by the newer player overlay implementation. */
private object NewPlayerOverlaysFeatureFlagFingerprint : Fingerprint(
    filters = listOf(literal(45752335L))
)

/** Feature flag used by the newer top-control layout. */
private object ModernPlayerTopControlsFeatureFlagFingerprint : Fingerprint(
    filters = listOf(literal(45750838L))
)

/** Flags that enable layout pieces incompatible with the legacy controls. */
private object PlayerControlsModernAccessibilityFeatureFlagFingerprint : Fingerprint(
    filters = listOf(literal(45757309L))
)

private object PlayerCommentTeaserFeatureFlagFingerprint : Fingerprint(
    filters = listOf(literal(45771730L))
)

private object RecyclerViewScrollingFeatureFlagFingerprint : Fingerprint(
    filters = listOf(literal(45763727L))
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
        if (!restoreOldPlayerButtonsSupported) return@execute

        addPreference(arrayOf("SETTINGS: RESTORE_OLD_PLAYER_BUTTONS"))

        NewPlayerOverlaysFeatureFlagFingerprint.matchAll().forEach {
            it.method.insertLiteralOverride(
                it.instructionMatches.first().index,
                "$EXTENSION_CLASS_DESCRIPTOR->useModernPlayerLayout(Z)Z",
            )
        }

        ModernPlayerTopControlsFeatureFlagFingerprint.matchAll().forEach {
            it.method.insertLiteralOverride(
                it.instructionMatches.first().index,
                "$EXTENSION_CLASS_DESCRIPTOR->useModernPlayerTopControls(Z)Z",
            )
        }

        val layoutFlagFingerprints = if (is_21_15_or_greater) {
            arrayOf(
                PlayerControlsModernAccessibilityFeatureFlagFingerprint,
                PlayerCommentTeaserFeatureFlagFingerprint,
                RecyclerViewScrollingFeatureFlagFingerprint,
            )
        } else {
            arrayOf(PlayerControlsModernAccessibilityFeatureFlagFingerprint)
        }
        layoutFlagFingerprints.forEach { fingerprint ->
            fingerprint.matchAll().forEach {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->allowModernPlayerLayoutFlags(Z)Z",
                )
            }
        }
    }
}
