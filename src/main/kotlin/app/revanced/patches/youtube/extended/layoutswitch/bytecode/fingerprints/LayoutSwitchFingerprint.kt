package app.revanced.patches.youtube.extended.layoutswitch.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object LayoutSwitchFingerprint : MethodFingerprint(
    "I", AccessFlags.PUBLIC or AccessFlags.STATIC, listOf("L"), listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_16,
        Opcode.IF_GE,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_16,
        Opcode.IF_GE,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_16,
        Opcode.IF_GE,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_4,
        Opcode.RETURN
    )
)
