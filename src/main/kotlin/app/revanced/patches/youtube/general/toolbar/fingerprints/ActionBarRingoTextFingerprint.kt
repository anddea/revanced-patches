package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.ActionBarRingoTextFingerprint.indexOfStartDelayInstruction
import app.revanced.patches.youtube.general.toolbar.fingerprints.ActionBarRingoTextFingerprint.indexOfStaticInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ActionBarRingoTextFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ ->
        indexOfStartDelayInstruction(methodDef) >= 0 &&
                indexOfStaticInstruction(methodDef) >= 0
    }
) {
    fun indexOfStartDelayInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "setStartDelay"
        }

    fun indexOfStaticInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstructionReversed(indexOfStartDelayInstruction(methodDef)) {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_STATIC &&
                    reference?.parameterTypes?.size == 1 &&
                    reference.parameterTypes.firstOrNull() == "Landroid/content/Context;" &&
                    reference.returnType == "Z"
        }
}
