package app.revanced.patches.youtube.layout.player.autoplaybutton.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object LayoutConstructorFingerprint : MethodFingerprint(
    strings = listOf("1.0x"),
    customFingerprint = { methodDef ->
        methodDef.definingClass.endsWith("YouTubeControlsOverlay;")
    }
)