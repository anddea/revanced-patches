package app.revanced.patches.youtube.video.information.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

/**
 * This fingerprint is compatible with all versions of YouTube starting from v18.29.38 to supported versions.
 * This method is invoked only in Shorts.
 * Accurate video information is invoked even when the user moves Shorts upward or downward.
 */
internal object VideoIdFingerprintShorts : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("Lcom/google/android/libraries/youtube/innertube/model/player/PlayerResponseModel;"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT
    ),
    literalSupplier = { 45365621 }
)