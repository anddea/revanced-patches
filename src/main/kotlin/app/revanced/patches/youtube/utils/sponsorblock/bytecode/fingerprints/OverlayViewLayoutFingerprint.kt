package app.revanced.patches.youtube.utils.sponsorblock.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.InsetOverlayViewLayout
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object OverlayViewLayoutFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST
    ),
    customFingerprint = { it, _ -> it.definingClass.endsWith("YouTubeControlsOverlay;") && it.isWideLiteralExists(InsetOverlayViewLayout) }
)