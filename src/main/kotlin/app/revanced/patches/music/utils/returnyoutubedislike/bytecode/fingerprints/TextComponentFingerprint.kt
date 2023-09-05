package app.revanced.patches.music.utils.returnyoutubedislike.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.ButtonIconPaddingMedium
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.Opcode

object TextComponentFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.CONST_HIGH16),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(ButtonIconPaddingMedium) }
)