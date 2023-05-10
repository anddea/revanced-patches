package app.revanced.patches.youtube.layout.fullscreen.flimstripoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.scrubbingId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object ScrubbingLabelFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.IPUT_BOOLEAN,
        Opcode.CONST_WIDE_32
    ),
    customFingerprint = { it.isWideLiteralExists(scrubbingId) }
)