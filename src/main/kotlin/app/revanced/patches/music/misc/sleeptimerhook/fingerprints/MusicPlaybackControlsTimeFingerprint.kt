package app.revanced.patches.music.misc.sleeptimerhook.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object MusicPlaybackControlsTimeFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    parameters = listOf("J","J","J","J"),
    opcodes = listOf(Opcode.RETURN_VOID),
)