package app.revanced.patches.shared.captions

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val storyboardRendererDecoderRecommendedLevelFingerprint = legacyFingerprint(
    name = "storyboardRendererDecoderRecommendedLevelFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("#-1#")
)

internal val subtitleTrackFingerprint = legacyFingerprint(
    name = "subtitleTrackFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("DISABLE_CAPTIONS_OPTION")
)
