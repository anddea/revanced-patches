package app.revanced.patches.music.utils.fix.parameter

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val subtitleWindowFingerprint = legacyFingerprint(
    name = "subtitleWindowFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("I", "I", "I", "Z", "Z"),
    strings = listOf("invalid anchorHorizontalPos: %s"),
)

/**
 * If this flag is activated, a playback issue occurs in age-restricted videos.
 */
internal const val AGE_RESTRICTED_PLAYBACK_FEATURE_FLAG = 45651506L

internal val ageRestrictedPlaybackFeatureFlagFingerprint = legacyFingerprint(
    name = "ageRestrictedPlaybackFeatureFlagFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(AGE_RESTRICTED_PLAYBACK_FEATURE_FLAG),
)
