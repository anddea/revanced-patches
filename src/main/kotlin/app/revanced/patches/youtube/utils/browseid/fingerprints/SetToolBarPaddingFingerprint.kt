package app.revanced.patches.youtube.utils.browseid.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ToolBarPaddingHome
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.Opcode

object SetToolBarPaddingFingerprint : LiteralValueFingerprint(
    returnType = "V",
    parameters = listOf("I", "I"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    literalSupplier = { ToolBarPaddingHome }
)