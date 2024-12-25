package app.revanced.patches.youtube.utils.fix.swiperefresh

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val swipeRefreshLayoutFingerprint = legacyFingerprint(
    name = "swipeRefreshLayoutFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.RETURN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN
    ),
    customFingerprint = { method, _ -> method.definingClass.endsWith("/SwipeRefreshLayout;") }
)

