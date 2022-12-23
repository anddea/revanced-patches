package app.revanced.patches.youtube.layout.general.personalinformation.bytecode.fingerprints

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.Opcode

@Name("account-switcher-accessibility-label-fingerprint")
@YouTubeCompatibility
@Version("0.0.1")
object AccountSwitcherAccessibilityLabelFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.NEW_ARRAY,
        Opcode.CONST_4,
        Opcode.APUT_OBJECT,
        Opcode.CONST,
    ),
    customFingerprint = { methodDef ->
        methodDef.implementation?.instructions?.any { instruction ->
            instruction.opcode.ordinal == Opcode.CONST.ordinal &&
            (instruction as? WideLiteralInstruction)?.wideLiteral == SharedResourcdIdPatch.accountSwitcherAccessibilityLabelId
        } == true
    }
)