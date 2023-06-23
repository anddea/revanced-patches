package app.revanced.patches.music.layout.compactdialog.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.DialogSolid
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object DialogSolidFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(DialogSolid) }
)

