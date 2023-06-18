package app.revanced.patches.youtube.utils.playercontrols.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object SpeedEduVisibleParentFingerprint : MethodFingerprint(
    returnType = "L",
    strings = listOf(", isSpeedmasterEDUVisible=")
)