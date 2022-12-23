package app.revanced.patches.youtube.misc.doublebacktoexit.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object RecyclerViewFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
    ),
    strings = listOf("Cannot scroll to position a LayoutManager set. Call setLayoutManager with a non-null argument."),
    customFingerprint = { methodDef ->
        methodDef.definingClass == "Landroid/support/v7/widget/RecyclerView;"
    }
)

