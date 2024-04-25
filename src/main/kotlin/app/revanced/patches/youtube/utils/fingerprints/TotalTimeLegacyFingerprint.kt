package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.TotalTime
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

object TotalTimeLegacyFingerprint : LiteralValueFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST
    ),
    literalSupplier = { TotalTime }
)