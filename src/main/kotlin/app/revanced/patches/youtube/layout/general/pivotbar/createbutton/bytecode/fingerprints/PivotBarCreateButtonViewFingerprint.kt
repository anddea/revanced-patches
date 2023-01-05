package app.revanced.patches.youtube.layout.general.pivotbar.createbutton.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object PivotBarCreateButtonViewFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    customFingerprint = { methodDef ->
        methodDef.implementation?.instructions?.any { instruction ->
            instruction.opcode.ordinal == Opcode.CONST.ordinal &&
            (instruction as? WideLiteralInstruction)?.wideLiteral == SharedResourcdIdPatch.imageOnlyTabId
        } == true
    }
)