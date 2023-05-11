package app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.insetOverlayViewLayoutId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object OverlayViewLayoutFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST
    ),
    customFingerprint = { it.definingClass.endsWith("YouTubeControlsOverlay;") && it.isWideLiteralExists(insetOverlayViewLayoutId) }
)