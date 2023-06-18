package app.revanced.patches.music.misc.videoid.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object MusicVideoIdFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { it, _ -> it.definingClass.endsWith("SubtitlesOverlayPresenter;")  && it.name == "handleVideoStageEvent" }
)
