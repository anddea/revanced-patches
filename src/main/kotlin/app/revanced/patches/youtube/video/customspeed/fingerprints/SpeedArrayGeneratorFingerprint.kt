package app.revanced.patches.youtube.video.customspeed.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object SpeedArrayGeneratorFingerprint : MethodFingerprint(
    returnType = "[L",
    access = AccessFlags.PUBLIC or AccessFlags.STATIC,
    strings = listOf("0.0#")
)
