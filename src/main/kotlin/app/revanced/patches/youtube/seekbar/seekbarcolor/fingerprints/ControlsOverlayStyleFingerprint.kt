package app.revanced.patches.youtube.seekbar.seekbarcolor.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object ControlsOverlayStyleFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.IGET_BOOLEAN),
    strings = listOf("supportsNextPrevious", "Missing required properties:")
)