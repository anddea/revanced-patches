package app.revanced.patches.music.misc.shuffle.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object MusicPlaybackControlsFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    opcodes = listOf(
        Opcode.IPUT_BOOLEAN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("MusicPlaybackControls;") }
)
