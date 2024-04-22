package app.revanced.patches.youtube.general.handle.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AccountSwitcherAccessibility
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

object AccountSwitcherAccessibilityLabelAlternativeFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.CONST,
        Opcode.FILLED_NEW_ARRAY,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET,
        Opcode.AND_INT_LIT8,
        Opcode.CONST_4,
        Opcode.CONST_16
    ),
    literalSupplier = { AccountSwitcherAccessibility }
)