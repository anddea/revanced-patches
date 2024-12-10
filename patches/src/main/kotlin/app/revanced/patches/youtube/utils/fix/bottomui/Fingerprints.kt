package app.revanced.patches.youtube.utils.fix.bottomui

import app.revanced.util.fingerprint.legacyFingerprint

internal val exploderControlsFingerprint = legacyFingerprint(
    name = "exploderControlsFingerprint",
    returnType = "Z",
    literals = listOf(45643739L),
)

internal val fullscreenButtonPositionFingerprint = legacyFingerprint(
    name = "fullscreenButtonPositionFingerprint",
    returnType = "Z",
    literals = listOf(45627640L),
)

internal val fullscreenButtonViewStubFingerprint = legacyFingerprint(
    name = "fullscreenButtonViewStubFingerprint",
    returnType = "Z",
    literals = listOf(45617294L),
)
