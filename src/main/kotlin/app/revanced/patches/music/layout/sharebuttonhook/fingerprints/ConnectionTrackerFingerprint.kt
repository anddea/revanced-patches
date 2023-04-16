package app.revanced.patches.music.layout.sharebuttonhook.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object ConnectionTrackerFingerprint : MethodFingerprint(
    returnType = "Z",
    opcodes = listOf(Opcode.THROW),
    strings = listOf("ConnectionTracker")
)
