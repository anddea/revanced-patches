package app.revanced.patches.shared.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object LayoutConstructorFingerprint : MethodFingerprint(
    strings = listOf("1.0x"),
    customFingerprint = { it.definingClass.endsWith("YouTubeControlsOverlay;") }
)