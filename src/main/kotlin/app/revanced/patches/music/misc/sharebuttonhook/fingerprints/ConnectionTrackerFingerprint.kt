package app.revanced.patches.music.misc.sharebuttonhook.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object ConnectionTrackerFingerprint : MethodFingerprint(
    returnType = "Z",
    opcodes = listOf(Opcode.THROW),
    strings = listOf("ConnectionTracker")
)
