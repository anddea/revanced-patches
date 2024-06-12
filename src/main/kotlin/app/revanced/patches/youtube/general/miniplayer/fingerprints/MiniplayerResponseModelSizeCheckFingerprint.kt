package app.revanced.patches.youtube.general.miniplayer.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

@Suppress("SpellCheckingInspection")
internal object MiniplayerResponseModelSizeCheckFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
    )
)