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
    45631257L  // Feed laid out behind the navigation bar instead of above it.
)

/**
 * Navigation bar also covers the system navigation buttons, the only one of
 * [TRANSLUCENT_NAVIGATION_BAR_FEATURE_FLAGS] that changes the room left for them.
 */
internal const val NAVIGATION_BAR_COVERS_SYSTEM_BUTTONS_FLAG = 45632194L

internal object TranslucentNavigationBarFeatureFlagsFingerprint : Fingerprint(
    filters = listOf(
        anyInstruction(*TRANSLUCENT_NAVIGATION_BAR_FEATURE_FLAGS.map { literal(it) }.toTypedArray())
    )
)

