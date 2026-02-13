package app.morphe.patches.youtube.general.loadingscreen

import app.morphe.util.fingerprint.legacyFingerprint

internal const val GRADIENT_LOADING_SCREEN_AB_CONSTANT = 45412406L

internal val useGradientLoadingScreenFingerprint = legacyFingerprint(
    name = "gradientLoadingScreenPrimaryFingerprint",
    literals = listOf(GRADIENT_LOADING_SCREEN_AB_CONSTANT),
)
