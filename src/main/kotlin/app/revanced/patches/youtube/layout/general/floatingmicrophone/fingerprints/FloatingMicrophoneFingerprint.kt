package app.revanced.patches.youtube.layout.general.floatingmicrophone.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.fabId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object FloatingMicrophoneFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { it.isWideLiteralExists(fabId) }
)