package app.morphe.patches.youtube.misc.backgroundplayback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.resourceid.backgroundCategory
import app.morphe.util.customLiteral
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object KidsBackgroundPlaybackPolicyControllerFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "L", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.IGET_OBJECT,
    ),
    custom = customLiteral { 5 }
)

internal val backgroundPlaybackManagerFingerprint = legacyFingerprint(
    name = "backgroundPlaybackManagerFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.AND_INT_LIT16),
    literals = listOf(64657230L),
)

internal val backgroundPlaybackSettingsFingerprint = legacyFingerprint(
    name = "backgroundPlaybackSettingsFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IF_NEZ,
        Opcode.GOTO
    ),
    literals = listOf(backgroundCategory),
)

internal val kidsBackgroundPlaybackPolicyControllerFingerprint = legacyFingerprint(
    name = "kidsBackgroundPlaybackPolicyControllerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I", "L", "L"),
    literals = listOf(5L),
)

internal val kidsBackgroundPlaybackPolicyControllerParentFingerprint = legacyFingerprint(
    name = "kidsBackgroundPlaybackPolicyControllerParentFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf(PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.SGET_OBJECT
                    && getReference<FieldReference>()?.name == "miniplayerRenderer"
        } >= 0
    }
)

internal val backgroundPlaybackManagerShortsFingerprint = legacyFingerprint(
    name = "backgroundPlaybackManagerShortsFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    returnType = "Z",
    parameters = listOf("L"),
    literals = listOf(151635310L),
)

internal val backgroundPlaybackManagerCairoFragmentParentFingerprint = legacyFingerprint(
    name = "backgroundPlaybackManagerCairoFragmentParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("yt_android_settings"),
    customFingerprint = { method, _ ->
        method.definingClass != "Lcom/google/android/apps/youtube/app/settings/AboutPrefsFragment;"
    }
)

/**
 * Matches using the class found in [backgroundPlaybackManagerCairoFragmentParentFingerprint].
 */
internal val backgroundPlaybackManagerCairoFragmentPrimaryFingerprint = legacyFingerprint(
    name = "backgroundPlaybackManagerCairoFragmentPrimaryFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.INVOKE_SUPER,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,  // Method of [cairoFragmentConfigFingerprint]
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.CONST_4,
        Opcode.IPUT_OBJECT,
    ),
)

/**
 * Matches using the class found in [backgroundPlaybackManagerCairoFragmentParentFingerprint].
 */
internal val backgroundPlaybackManagerCairoFragmentSecondaryFingerprint = legacyFingerprint(
    name = "backgroundPlaybackManagerCairoFragmentSecondaryFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.INVOKE_SUPER,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,  // Method of [cairoFragmentConfigFingerprint]
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.NEW_INSTANCE,
    ),
)

internal const val PIP_INPUT_CONSUMER_FEATURE_FLAG = 45638483L

/**
 * Fix 'E/InputDispatcher: Window handle pip_input_consumer has no registered input channel'
 * Related with [ReVanced_Extended#2764](https://github.com/inotia00/ReVanced_Extended/issues/2764).
 */
internal val pipInputConsumerFeatureFlagFingerprint = legacyFingerprint(
    name = "pipInputConsumerFeatureFlagFingerprint",
    literals = listOf(PIP_INPUT_CONSUMER_FEATURE_FLAG),
)

internal const val SHORTS_BACKGROUND_PLAYBACK_FEATURE_FLAG = 45415425L

internal val shortsBackgroundPlaybackFeatureFlagFingerprint = legacyFingerprint(
    name = "shortsBackgroundPlaybackFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(SHORTS_BACKGROUND_PLAYBACK_FEATURE_FLAG),
)