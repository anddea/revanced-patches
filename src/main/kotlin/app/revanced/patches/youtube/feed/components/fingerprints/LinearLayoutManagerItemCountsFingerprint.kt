package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object LinearLayoutManagerItemCountsFingerprint : MethodFingerprint(
    returnType = "I",
    accessFlags = AccessFlags.FINAL.value,
    parameters = listOf("L", "L", "L", "Z"),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IF_LEZ,
        Opcode.INVOKE_VIRTUAL,
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Landroid/support/v7/widget/LinearLayoutManager;"
    }
)
