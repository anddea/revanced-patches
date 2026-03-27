package app.morphe.patches.youtube.utils.engagement

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val engagementPanelBuilderFingerprint = legacyFingerprint(
    name = "engagementPanelBuilderFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L", "L", "Z", "Z"),
    strings = listOf(
        "EngagementPanelController: cannot show EngagementPanel before EngagementPanelController.init() has been called.",
        "[EngagementPanel] Cannot show EngagementPanel before EngagementPanelController.init() has been called."
    )
)

internal val engagementPanelLayoutFingerprint = legacyFingerprint(
    name = "engagementPanelLayoutFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L", "I"),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>().toString() == "Landroid/widget/FrameLayout;->indexOfChild(Landroid/view/View;)I"
        } >= 0
    }
)

internal val engagementPanelUpdateFingerprint = legacyFingerprint(
    name = "engagementPanelUpdateFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L", "Z"),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>().toString() == "Ljava/util/ArrayDeque;->pop()Ljava/lang/Object;"
        } >= 0
    }
)
