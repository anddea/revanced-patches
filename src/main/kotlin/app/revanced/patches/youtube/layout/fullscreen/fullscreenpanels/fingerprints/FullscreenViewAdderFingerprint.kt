package app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object FullscreenViewAdderFingerprint : MethodFingerprint(
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQ,
        Opcode.GOTO,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL
    ),
    customFingerprint = { it.definingClass.endsWith("FullscreenEngagementPanelOverlay;") }
)