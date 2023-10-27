package app.revanced.patches.youtube.utils.settings.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.Appearance
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.Opcode

object ThemeSetterSystemFingerprint : MethodFingerprint(
    returnType = "L",
    opcodes = listOf(Opcode.RETURN_OBJECT),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(Appearance) }
)