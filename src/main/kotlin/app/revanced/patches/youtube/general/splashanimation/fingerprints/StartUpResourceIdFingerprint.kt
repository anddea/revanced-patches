package app.revanced.patches.youtube.general.splashanimation.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.containsWideLiteralInstructionValue
import com.android.tools.smali.dexlib2.AccessFlags

internal object StartUpResourceIdFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("I"),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(3)
                && methodDef.containsWideLiteralInstructionValue(4)
    }
)