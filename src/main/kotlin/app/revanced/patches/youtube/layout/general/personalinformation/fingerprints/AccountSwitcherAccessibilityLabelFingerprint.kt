package app.revanced.patches.youtube.layout.general.personalinformation.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.accountSwitcherAccessibilityId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object AccountSwitcherAccessibilityLabelFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.NEW_ARRAY,
        Opcode.CONST_4,
        Opcode.APUT_OBJECT,
        Opcode.CONST
    ),
    customFingerprint = { it.isWideLiteralExists(accountSwitcherAccessibilityId) }
)