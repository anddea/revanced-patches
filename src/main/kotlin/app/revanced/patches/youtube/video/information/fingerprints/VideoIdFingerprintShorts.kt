package app.revanced.patches.youtube.video.information.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.PlayerResponseModelUtils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.util.containsWideLiteralInstructionValue
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/**
 * This fingerprint is compatible with all versions of YouTube starting from v18.29.38 to supported versions.
 * This method is invoked only in Shorts.
 * Accurate video information is invoked even when the user moves Shorts upward or downward.
 */
internal object VideoIdFingerprintShorts : MethodFingerprint(
    returnType = "V",
    parameters = listOf(PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = custom@{ methodDef, _ ->
        if (methodDef.containsWideLiteralInstructionValue(45365621))
            return@custom true

        methodDef.indexOfFirstInstruction {
            getReference<FieldReference>()?.name == "reelWatchEndpoint"
        } >= 0
    }
)