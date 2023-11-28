package app.revanced.patches.youtube.utils.browseid.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ToolBarPaddingHome
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.Opcode

object SetToolBarPaddingFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("I", "I"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(ToolBarPaddingHome) }
)