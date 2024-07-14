package app.revanced.patches.youtube.video.information.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.containsWideLiteralInstructionIndex
import app.revanced.util.getTargetIndexWithFieldReferenceName
import com.android.tools.smali.dexlib2.Opcode

/**
 * This fingerprint is compatible with all versions of YouTube starting from v18.29.38 to supported versions.
 * This method is invoked only in Shorts.
 * Accurate video information is invoked even when the user moves Shorts upward or downward.
 */
internal object VideoIdFingerprintShorts : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Lcom/google/android/libraries/youtube/innertube/model/player/PlayerResponseModel;"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = custom@{ methodDef, _ ->
        if (methodDef.containsWideLiteralInstructionIndex(45365621))
            return@custom true

        methodDef.getTargetIndexWithFieldReferenceName("reelWatchEndpoint") >= 0
    }
)