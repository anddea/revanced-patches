package app.revanced.patches.music.video.videoid.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object VideoIdFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/String;"),
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE_RANGE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE_RANGE,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    strings = listOf("Null initialPlayabilityStatus")
)
