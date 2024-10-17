package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

internal object EngagementPanelHeightParentFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(Opcode.NEW_ARRAY),
    parameters = emptyList(),
    customFingerprint = custom@{ methodDef, _ ->
        if (methodDef.definingClass.startsWith("Lcom/")) {
            return@custom false
        }
        if (methodDef.returnType == "Ljava/lang/Object;") {
            return@custom false
        }

        methodDef.indexOfFirstInstruction {
            opcode == Opcode.CHECK_CAST &&
                    (this as? ReferenceInstruction)?.reference?.toString() == "Lcom/google/android/libraries/youtube/engagementpanel/size/EngagementPanelSizeBehavior;"
        } >= 0
    }
)
