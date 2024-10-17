package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.containsWideLiteralInstructionValue
import com.android.tools.smali.dexlib2.AccessFlags

internal object ShortsTimeStampPrimaryFingerprint : MethodFingerprint(
    returnType = "I",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I"),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(45627350)
                && methodDef.containsWideLiteralInstructionValue(45638282)
                && methodDef.containsWideLiteralInstructionValue(10002)
    },
)