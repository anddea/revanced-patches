package app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object VideoTimeFingerprint : MethodFingerprint (
    "V", AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR, listOf("J", "J", "J", "J", "I", "L"), listOf(
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
    )
)