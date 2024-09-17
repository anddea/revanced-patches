package app.revanced.patches.youtube.utils.returnyoutubedislike.rollingnumber.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * This fingerprint is compatible with YouTube v18.30.xx+
 */
internal object RollingNumberMeasureAnimatedTextFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.ADD_FLOAT_2ADDR, // measuredTextWidth
        Opcode.ADD_INT_LIT8,
        Opcode.GOTO
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            getReference<MethodReference>()?.toString() == "Landroid/text/TextPaint;->measureText([CII)F"
        } >= 0
    }
)