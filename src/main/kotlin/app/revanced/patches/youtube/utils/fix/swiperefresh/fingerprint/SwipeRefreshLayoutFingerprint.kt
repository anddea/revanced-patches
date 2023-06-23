package app.revanced.patches.youtube.utils.fix.swiperefresh.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object SwipeRefreshLayoutFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.RETURN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN
    ),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("SwipeRefreshLayout;") }
)
