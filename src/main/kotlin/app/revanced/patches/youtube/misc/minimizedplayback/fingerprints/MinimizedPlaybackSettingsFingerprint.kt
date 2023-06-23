package app.revanced.patches.youtube.misc.minimizedplayback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.BackgroundCategory
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object MinimizedPlaybackSettingsFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        Opcode.IF_NEZ,
        Opcode.GOTO
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(BackgroundCategory) }
)
