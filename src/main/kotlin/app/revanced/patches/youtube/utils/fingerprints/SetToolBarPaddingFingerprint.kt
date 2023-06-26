package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ToolBarPaddingHome
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

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