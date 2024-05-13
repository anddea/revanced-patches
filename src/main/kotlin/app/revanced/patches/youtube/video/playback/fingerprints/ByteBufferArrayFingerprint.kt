package app.revanced.patches.youtube.video.playback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ByteBufferArrayFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "I",
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.SHL_INT_LIT8,
        Opcode.SHL_INT_LIT8,
        Opcode.OR_INT_2ADDR,
        Opcode.SHL_INT_LIT8,
        Opcode.OR_INT_2ADDR,
        Opcode.OR_INT_2ADDR,
        Opcode.RETURN
    )
)
