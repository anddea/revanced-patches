/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.misc.backgroundplayback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object AutomaticForegroundPlaybackResumeFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45770945L),
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
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        opcode(Opcode.GOTO, location = MatchAfterImmediately()),
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
