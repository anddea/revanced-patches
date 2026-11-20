/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 *
 * Modified by lavinhoque33, 2026-10-21.
 */

package app.morphe.patches.youtube.misc.backgroundplayback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val AUTOMATIC_FOREGROUND_PLAYBACK_RESUME_FLAG = 45770945L

internal object AutomaticForegroundPlaybackResumeFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(AUTOMATIC_FOREGROUND_PLAYBACK_RESUME_FLAG)
    )
)

/**
 * Saves and restores if the video was playing when the app went to the background. Its class and
 * the class of the main activity that restores it read the automatic foreground playback resume flag
 * when the app goes to the background and returns to the foreground.
 */
internal object PlayingWhenBackgroundedFingerprint : Fingerprint(
    filters = listOf(
        string("was_playing_when_backgrounded")
    )
)

internal object AutomaticPlaybackPausedInFlyoutFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45741823L),
    )
)

internal object KidsBackgroundPlaybackPolicyControllerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "L", "L"),
    filters = listOf(
        literal(5L),
    ) + OpcodesFilter.opcodesToFilters(
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
    )
)

internal object BackgroundPlaybackManagerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L"),
    filters = listOf(
        opcode(Opcode.AND_INT_LIT16),
        literal(64657230L),
    )
)

internal object BackgroundPlaybackSettingsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        opcode(Opcode.INVOKE_VIRTUAL),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.INVOKE_VIRTUAL, location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        // 21.39: the IF_NEZ + GOTO following IF_EQZ no longer exist (code was reordered).
        resourceLiteral(ResourceType.STRING, "pref_background_and_offline_category"),
    )
)

internal object BackgroundPlaybackManagerShortsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L"),
    filters = listOf(
        literal(151635310L),
        opcode(Opcode.IGET_BOOLEAN, location = MatchAfterWithin(8)),
    )
)

internal object BackgroundPlaybackManagerCairoFragmentParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("yt_android_settings"),
    custom = { method, _ ->
        method.definingClass != "Lcom/google/android/apps/youtube/app/settings/AboutPrefsFragment;"
    }
)

/**
 * Matches using the class found in [BackgroundPlaybackManagerCairoFragmentParentFingerprint].
 */
internal object BackgroundPlaybackManagerCairoFragmentPrimaryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_SUPER,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,  // Method of [cairoFragmentConfigFingerprint]
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IPUT_OBJECT,
    )
)

/**
 * Matches using the class found in [BackgroundPlaybackManagerCairoFragmentParentFingerprint].
 */
internal object BackgroundPlaybackManagerCairoFragmentSecondaryFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_SUPER,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,  // Method of [cairoFragmentConfigFingerprint]
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.NEW_INSTANCE,
    )
)

internal const val CAIRO_FRAGMENT_PIP_FEATURE_FLAG = 45532100L

// 21.39: the boolean getter that [BackgroundPlaybackManagerCairoFragmentPrimaryFingerprint] and
// [BackgroundPlaybackManagerCairoFragmentSecondaryFingerprint] relied on is inlined into the
// two lifecycle methods (onStart/onResume equivalents) as `const-wide/32 flag` + `invoke-virtual w(JZ)Z`.
// Matches both methods inside the class found by [BackgroundPlaybackManagerCairoFragmentParentFingerprint].
internal object BackgroundPlaybackManagerCairoFragmentInlinedFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        opcode(Opcode.INVOKE_SUPER),
        literal(CAIRO_FRAGMENT_PIP_FEATURE_FLAG),
    )
)

internal const val PIP_INPUT_CONSUMER_FEATURE_FLAG = 45638483L

/**
 * Fix 'E/InputDispatcher: Window handle pip_input_consumer has no registered input channel'
 * Related with [ReVanced_Extended#2764](https://github.com/inotia00/ReVanced_Extended/issues/2764).
 */
internal object PipInputConsumerFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(PIP_INPUT_CONSUMER_FEATURE_FLAG),
    )
)

internal const val SHORTS_BACKGROUND_PLAYBACK_FEATURE_FLAG = 45415425L

internal object ShortsBackgroundPlaybackFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        literal(SHORTS_BACKGROUND_PLAYBACK_FEATURE_FLAG),
    )
)

/**
 * Sets the artwork of the state of the playback media session, such as the thumbnail of the playback notification,
 * and returns the change of the state, or 0 if there is no artwork and there was none before.
 */
internal object MediaSessionArtworkStateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "I",
    parameters = listOf("Landroid/graphics/Bitmap;", "Landroid/graphics/Bitmap;"),
    filters = listOf(
        opcode(Opcode.IF_NEZ),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/graphics/Bitmap;", location = MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/graphics/Bitmap;", location = MatchAfterImmediately()),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        // No change.
        opcode(Opcode.CONST_4, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Landroid/graphics/Bitmap;"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, type = "Landroid/graphics/Bitmap;", location = MatchAfterImmediately()),
        // Change of the artwork.
        opcode(Opcode.CONST_16, location = MatchAfterImmediately())
    ),
    // The state class implements the interface of the state. Some versions have a class
    // with the same method for a second state, which implements nothing.
    custom = { _, classDef ->
        classDef.interfaces.isNotEmpty()
    }
)

/**
 * Returns if the background playback setting of YouTube (Settings > Background & downloads) allows
 * background playback now: always, or only with headphones and headphones are connected.
 */
internal object BackgroundPlaybackSettingAllowedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf("L", "L", "L"),
    filters = listOf(
        methodCall(definingClass = "Landroid/media/AudioManager;", name = "isWiredHeadsetOn"),
        methodCall(definingClass = "Landroid/media/AudioManager;", name = "isBluetoothA2dpOn"),
        methodCall(definingClass = "Landroid/media/AudioManager;", name = "isBluetoothScoOn")
    )
)

/**
 * Calls of the flag accessor of [AutomaticForegroundPlaybackResumeFeatureFlagFingerprint] in a class.
 * 21.29 and older only.
 */
context(patchContext: app.morphe.patcher.patch.BytecodePatchContext)
internal fun automaticForegroundPlaybackResumeFlagAccessorCallFingerprint(classType: String) = Fingerprint(
    definingClass = classType,
    filters = listOf(
        methodCall(reference = AutomaticForegroundPlaybackResumeFeatureFlagFingerprint.originalMethod)
    )
)

/**
 * Reads the background playback setting of the account and calls [BackgroundPlaybackSettingAllowedFingerprint].
 */
context(patchContext: app.morphe.patcher.patch.BytecodePatchContext)
internal fun backgroundPlaybackSettingFingerprint() = Fingerprint(
    classFingerprint = BackgroundPlaybackSettingAllowedFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        // Setting store of the account.
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            returnType = "Lcom/google/protobuf/MessageLite;",
            parameters = listOf()
        ),
        methodCall(reference = BackgroundPlaybackSettingAllowedFingerprint.originalMethod)
    )
)

/**
 * Constructor of the class of [backgroundPlaybackSettingFingerprint]. The class is merged with other classes,
 * and this is the constructor that takes the setting store and the audio manager the setting method reads.
 * The number of other parameters differs between app versions.
 */
internal fun backgroundPlaybackSettingConstructorFingerprint(
    settingClassType: String,
    settingStoreType: String,
    audioManagerType: String
) = Fingerprint(
    definingClass = settingClassType,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    filters = listOf(
        // Super constructor.
        methodCall(opcode = Opcode.INVOKE_DIRECT, name = "<init>")
    ),
    custom = { method, _ ->
        val parameters = method.parameterTypes
        parameters.size >= 2 &&
                parameters[0].toString() == settingStoreType &&
                parameters[1].toString() == audioManagerType
    }
)
