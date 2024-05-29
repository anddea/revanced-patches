package app.revanced.patches.youtube.general.splashanimation.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object StartUpResourceIdParentFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.DECLARED_SYNCHRONIZED,
    parameters = listOf("I", "I"),
    strings = listOf("early type", "final type")
)