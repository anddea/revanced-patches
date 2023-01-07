package app.revanced.patches.youtube.video.speed.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object VideoSpeedParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT")
)