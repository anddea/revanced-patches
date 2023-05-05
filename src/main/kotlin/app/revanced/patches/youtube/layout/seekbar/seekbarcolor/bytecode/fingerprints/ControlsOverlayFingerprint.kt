package app.revanced.patches.youtube.layout.seekbar.seekbarcolor.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object ControlsOverlayFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = {it.name == "<init>"}
)