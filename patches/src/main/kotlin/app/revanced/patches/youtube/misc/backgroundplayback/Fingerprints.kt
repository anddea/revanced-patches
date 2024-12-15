package app.revanced.patches.youtube.misc.backgroundplayback

import app.revanced.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.backgroundCategory
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

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

internal val shortsBackgroundPlaybackFeatureFlagFingerprint = legacyFingerprint(
    name = "shortsBackgroundPlaybackFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(45415425L),
)