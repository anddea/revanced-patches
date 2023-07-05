package app.revanced.patches.youtube.utils.fix.parameter.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object SubtitleWindowFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("I", "I", "I", "Z", "Z"),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/SubtitleWindowSettings;") }
)