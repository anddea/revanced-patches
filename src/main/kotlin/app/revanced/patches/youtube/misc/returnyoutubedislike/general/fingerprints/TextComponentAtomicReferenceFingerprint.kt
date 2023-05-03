package app.revanced.patches.youtube.misc.returnyoutubedislike.general.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object TextComponentAtomicReferenceFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.MOVE_OBJECT
    )
)