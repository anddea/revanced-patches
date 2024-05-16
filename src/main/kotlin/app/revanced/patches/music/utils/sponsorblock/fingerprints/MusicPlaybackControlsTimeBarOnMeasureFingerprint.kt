package app.revanced.patches.music.utils.sponsorblock.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object MusicPlaybackControlsTimeBarOnMeasureFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/MusicPlaybackControlsTimeBar;")
                && methodDef.name == "onMeasure"
    }
)