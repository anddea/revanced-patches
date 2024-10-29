package app.revanced.patches.youtube.player.descriptions.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.player.descriptions.fingerprints.EngagementPanelTitleFingerprint.indexOfContentDescriptionInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object EngagementPanelTitleFingerprint : MethodFingerprint(
    strings = listOf(". "),
    customFingerprint = { methodDef, _ ->
        indexOfContentDescriptionInstruction(methodDef) >= 0
    }
) {
    fun indexOfContentDescriptionInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstructionReversed {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "setContentDescription"
        }
}