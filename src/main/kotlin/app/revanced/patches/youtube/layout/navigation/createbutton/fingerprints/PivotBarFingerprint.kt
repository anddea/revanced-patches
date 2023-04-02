package app.revanced.patches.youtube.layout.navigation.createbutton.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object PivotBarFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT
    ),
    customFingerprint = { methodDef ->
        methodDef.definingClass == "Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotBar;" &&
                methodDef.implementation?.instructions?.any {
                    it.opcode.ordinal == Opcode.CONST.ordinal &&
                            (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.imageWithTextTabId
                } == true
    }
)