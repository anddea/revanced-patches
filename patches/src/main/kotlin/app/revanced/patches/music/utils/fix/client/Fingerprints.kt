package app.revanced.patches.music.utils.fix.client

import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * This is the fingerprint used in the 'client-spoof' patch around 2022.
 * (Integrated into [baseSpoofUserAgentPatch] now.)
 *
 * This method is modified by [baseSpoofUserAgentPatch], so the fingerprint does not check the [Opcode].
 */
internal val userAgentHeaderBuilderFingerprint = legacyFingerprint(
    name = "userAgentHeaderBuilderFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("(Linux; U; Android "),
)

/**
 * If this flag is activated, a playback issue occurs.
 * (Regardless of the 'Spoof client')
 *
 * Added in YouTube Music 7.33+
 */
internal const val PLAYBACK_FEATURE_FLAG = 45665455L

internal val playbackFeatureFlagFingerprint = legacyFingerprint(
    name = "playbackFeatureFlagFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(PLAYBACK_FEATURE_FLAG),
)

/**
 * If this flag is activated, a playback issue occurs.
 * (Regardless of the 'Spoof client')
 *
 * YouTube Music 7.16-8.30
 */
internal const val FALLBACK_FEATURE_FLAG = 45636987L

internal val fallbackFeatureFlagFingerprint = legacyFingerprint(
    name = "fallbackFeatureFlagFingerprint",
    returnType = "V",
    parameters = emptyList(),
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(FALLBACK_FEATURE_FLAG),
)

/**
 * If this flag is activated, a playback issue occurs.
 * (Regardless of the 'Spoof client')
 *
 * YouTube Music 8.12-8.30
 */
internal const val FORMATS_FEATURE_FLAG = 45680795L

internal val formatsFeatureFlagFingerprint = legacyFingerprint(
    name = "formatsFeatureFlagFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(FORMATS_FEATURE_FLAG),
)

internal val spoofAppVersionFingerprint = legacyFingerprint(
    name = "spoofAppVersionFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Landroid/content/Context;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.GOTO,
        Opcode.CONST_STRING,
    ),
    strings = listOf("pref_override_build_version_name"),
)

internal const val INIT_PLAYER_RESPONSE = " interstitialPlayerResponse="

/**
 * Inspired by the August 2024 commit:
 * https://github.com/inotia00/revanced-patches/commit/dde5331ba949ed2655ae168a6bc2485ebec197e9
 * Class 'Lcom/google/android/libraries/youtube/innertube/model/player/PlayerResponseModel;'
 * is obfuscated in YouTube Music, so this fingerprint is used to find the class 'PlayerResponseModel'
 */
internal val directorSavedStateToStringFingerprint = legacyFingerprint(
    name = "directorSavedStateToStringFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf(INIT_PLAYER_RESPONSE),
    customFingerprint = { method, _ ->
        method.name == "toString"
    }
)