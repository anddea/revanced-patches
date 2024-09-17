package app.revanced.patches.music.video.information.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.video.information.fingerprints.VideoLengthFingerprint.indexOfInvalidateInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object VideoLengthFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE
    ),
    customFingerprint = { methodDef, _ ->
        indexOfInvalidateInstruction(methodDef) >= 0
    }
) {
    fun indexOfInvalidateInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstructionReversed {
            getReference<MethodReference>()?.name == "invalidate"
        }
}