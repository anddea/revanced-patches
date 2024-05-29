package app.revanced.patches.youtube.player.descriptions.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint

internal object EngagementPanelTitleFingerprint : MethodReferenceNameFingerprint(
    strings = listOf(". "),
    reference = { "setContentDescription" }
)