package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ElementParserFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L", "[B", "L", "L"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.RETURN_OBJECT
    )
)