package app.revanced.patches.youtube.layout.fullscreen.flimstripoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.accessibilityVideoTimeId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object ScrubbingLabelAlternativeFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IPUT_BOOLEAN,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { it.isWideLiteralExists(accessibilityVideoTimeId) }
)