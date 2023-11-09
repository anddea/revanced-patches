package app.revanced.patches.youtube.player.speedoverlay.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists
import com.android.tools.smali.dexlib2.Opcode

/**
 * This value restores the 'Slide to seek' behavior.
 */
object RestoreSlideToSeekBehaviorFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    opcodes = listOf(Opcode.MOVE_RESULT),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45411329) }
)