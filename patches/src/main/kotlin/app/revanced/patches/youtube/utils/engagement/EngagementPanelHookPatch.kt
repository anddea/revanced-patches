package app.revanced.patches.youtube.utils.engagement

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.engagementPanelBuilderFingerprint
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private lateinit var hideEngagementPanelMethod: MutableMethod
private var showEngagementPanelMethods = mutableListOf<MutableMethod>()

val engagementPanelHookPatch = bytecodePatch(
    description = "engagementPanelHookPatch"
) {
    dependsOn(sharedResourceIdPatch)

    execute {
        engagementPanelBuilderFingerprint.matchOrThrow().let {
            it.classDef.methods.filter { method ->
                method.indexOfEngagementPanelBuilderInstruction(it.method) >= 0
            }.forEach { method ->
                showEngagementPanelMethods.add(method)
            }
        }

        hideEngagementPanelMethod =
            engagementPanelUpdateFingerprint.methodOrThrow(engagementPanelBuilderFingerprint)
    }
}

private fun Method.indexOfEngagementPanelBuilderInstruction(targetMethod: MutableMethod) =
    indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_DIRECT &&
                MethodUtil.methodSignaturesMatch(
                    targetMethod,
                    getReference<MethodReference>()!!
                )
    }

internal fun hookEngagementPanelState(classDescriptor: String) {
    showEngagementPanelMethods.forEach { method ->
        method.apply {
            val index = indexOfEngagementPanelBuilderInstruction(this)
            val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

            addInstruction(
                index + 2,
                "invoke-static {v$register}, $classDescriptor->showEngagementPanel(Ljava/lang/Object;)V"
            )
        }
    }

    hideEngagementPanelMethod.addInstruction(
        0,
        "invoke-static {}, $classDescriptor->hideEngagementPanel()V"
    )
}
