package app.revanced.patches.youtube.extended.forcevp9.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object Vp9PropsParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Android Wear")
)
