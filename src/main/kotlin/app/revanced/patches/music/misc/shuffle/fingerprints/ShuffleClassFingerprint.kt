package app.revanced.patches.music.misc.shuffle.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.DisabledIconAlpha
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object ShuffleClassFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IPUT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "<init>" && methodDef.isWideLiteralExists(
            DisabledIconAlpha
        )
    }
)

