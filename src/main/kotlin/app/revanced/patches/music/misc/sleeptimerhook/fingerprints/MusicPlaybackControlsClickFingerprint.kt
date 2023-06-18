package app.revanced.patches.music.misc.sleeptimerhook.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object MusicPlaybackControlsClickFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { it, _ -> it.definingClass.endsWith("/MusicPlaybackControls;") && it.name == "onClick" }
)
