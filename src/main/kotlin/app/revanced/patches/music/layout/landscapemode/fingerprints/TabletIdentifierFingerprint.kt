package app.revanced.patches.music.layout.landscapemode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.IsTablet
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object TabletIdentifierFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(IsTablet) }
)

