package app.revanced.patches.reddit.layout.navigation

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val bottomNavScreenFingerprint = legacyFingerprint(
    name = "bottomNavScreenFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, classDef ->
        method.name == "onGlobalLayout" &&
                classDef.type.startsWith("Lcom/reddit/launch/bottomnav/BottomNavScreen\$")
    }
)
