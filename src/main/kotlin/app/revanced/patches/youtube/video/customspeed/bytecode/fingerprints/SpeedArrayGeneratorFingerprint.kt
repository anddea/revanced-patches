package app.revanced.patches.youtube.video.customspeed.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

@FuzzyPatternScanMethod(2)
object SpeedArrayGeneratorFingerprint : MethodFingerprint(
    "[L",
    AccessFlags.PUBLIC or AccessFlags.STATIC,
    null,
    listOf(
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.GOTO,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
    ),
    listOf("0.0#")
)
