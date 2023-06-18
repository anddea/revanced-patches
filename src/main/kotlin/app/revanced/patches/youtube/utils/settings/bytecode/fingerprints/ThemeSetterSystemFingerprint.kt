package app.revanced.patches.youtube.utils.settings.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.Appearance
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object ThemeSetterSystemFingerprint : MethodFingerprint(
    returnType = "L",
    opcodes = listOf(Opcode.RETURN_OBJECT),
    customFingerprint = { it, _ -> it.isWideLiteralExists(Appearance) }
)