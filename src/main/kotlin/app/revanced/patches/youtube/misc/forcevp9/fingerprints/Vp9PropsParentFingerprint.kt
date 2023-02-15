package app.revanced.patches.youtube.misc.forcevp9.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object Vp9PropsParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Android Wear")
)
