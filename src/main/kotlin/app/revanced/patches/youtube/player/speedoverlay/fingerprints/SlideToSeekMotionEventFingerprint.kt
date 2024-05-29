package app.revanced.patches.youtube.player.speedoverlay.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SlideToSeekMotionEventFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;", "Landroid/view/MotionEvent;"),
    opcodes = listOf(
        Opcode.SUB_FLOAT_2ADDR,
        Opcode.INVOKE_VIRTUAL,  // SlideToSeek Boolean method
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,     // insert index
        Opcode.INVOKE_VIRTUAL
    )
)