package app.revanced.patches.youtube.layout.seekbar.seekbarcolor.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object ControlsOverlayParentFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PRIVATE or AccessFlags.STATIC or AccessFlags.FINAL,
    strings = listOf("Error screen presenter should be present")
)