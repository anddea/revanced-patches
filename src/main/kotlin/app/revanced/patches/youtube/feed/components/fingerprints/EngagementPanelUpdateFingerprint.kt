package app.revanced.patches.youtube.feed.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object EngagementPanelUpdateFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L", "Z"),
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>().toString() == "Ljava/util/ArrayDeque;->pop()Ljava/lang/Object;"
        } >= 0
    }
)
