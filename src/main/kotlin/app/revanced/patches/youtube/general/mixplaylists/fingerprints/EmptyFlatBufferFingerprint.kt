package app.revanced.patches.youtube.general.mixplaylists.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object EmptyFlatBufferFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL_RANGE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ
    ),
    strings = listOf("Failed to convert Element to Flatbuffers: %s")
)