package app.revanced.patches.youtube.utils.settings

import app.revanced.patches.youtube.utils.resourceid.appearance
import app.revanced.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal val themeSetterSystemFingerprint = legacyFingerprint(
    name = "themeSetterSystemFingerprint",
    returnType = "L",
    opcodes = listOf(Opcode.RETURN_OBJECT),
    literals = listOf(appearance),
)
