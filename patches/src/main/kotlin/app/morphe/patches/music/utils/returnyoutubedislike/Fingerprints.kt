package app.morphe.patches.music.utils.returnyoutubedislike

import app.morphe.patches.music.utils.resourceid.buttonIconPaddingMedium
import app.morphe.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal val textComponentFingerprint = legacyFingerprint(
    name = "textComponentFingerprint",
    returnType = "V",
    opcodes = listOf(Opcode.CONST_HIGH16),
    literals = listOf(buttonIconPaddingMedium),
)
