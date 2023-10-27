package app.revanced.patches.music.utils.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ButtonIconPaddingMedium
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.Opcode

object TextComponentFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.CONST_HIGH16),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(ButtonIconPaddingMedium) }
)