/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.literal

/**
 * Feature flags of the translucent navigation bar. 21.29 and older read each of them with a single
 * accessor method, newer targets read them inline in many classes, sometimes more than once in
 * the same method.
 */
internal val TRANSLUCENT_NAVIGATION_BAR_FEATURE_FLAGS = listOf(
    45630927L, // Blurred, translucent background of the navigation bar.
    NAVIGATION_BAR_COVERS_SYSTEM_BUTTONS_FLAG,
    FEED_BEHIND_NAVIGATION_BAR_FLAG
)

/**
 * Navigation bar also covers the system navigation buttons, which adds the room for them
 * to the navigation bar height.
 */
internal const val NAVIGATION_BAR_COVERS_SYSTEM_BUTTONS_FLAG = 45632194L

/**
 * Feed laid out behind the navigation bar instead of above it. The page then ends at the bottom
 * of the screen, and its list gets the navigation bar height as bottom padding.
 */
internal const val FEED_BEHIND_NAVIGATION_BAR_FLAG = 45631257L

internal object TranslucentNavigationBarFeatureFlagsFingerprint : Fingerprint(
    filters = listOf(
        anyInstruction(*TRANSLUCENT_NAVIGATION_BAR_FEATURE_FLAGS.map { literal(it) }.toTypedArray())
    )
)

/**
 * Draws the top bars behind the status bar. 21.29 and older read it with a single
 * accessor method, newer targets read it inline in every class that uses it.
 */
internal object EdgeToEdgeTopBarFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45400535L)
    )
)

