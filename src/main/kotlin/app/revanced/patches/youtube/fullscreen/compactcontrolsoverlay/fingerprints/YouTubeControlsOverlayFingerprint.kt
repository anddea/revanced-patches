package app.revanced.patches.youtube.fullscreen.compactcontrolsoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.YoutubeControlsOverlay
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object YouTubeControlsOverlayFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    ),
    customFingerprint = { it, _ -> it.definingClass.endsWith("YouTubeControlsOverlay;") && it.isWideLiteralExists(YoutubeControlsOverlay) }
)