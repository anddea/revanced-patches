package app.morphe.patches.youtube.utils.engagement

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.utils.extension.Constants.SHARED_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.util.findFreeRegister
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/EngagementPanel;"

internal lateinit var engagementPanelBuilderMethod: MutableMethod
internal var engagementPanelFreeRegister = 0
internal var engagementPanelIdIndex = 0
internal var engagementPanelIdRegister = 0
internal var engagementPanelIdInstruction = ""

val engagementPanelHookPatch = bytecodePatch(
    description = "engagementPanelHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
    )

    execute {
        engagementPanelControllerFingerprint.let {
            it.method.apply {
                val panelIdField =
                    it.instructionMatches.last().instruction.getReference<FieldReference>()!!
                val insertIndex = it.instructionMatches[5].index

                val (freeRegister, panelRegister) =
                    getInstruction<TwoRegisterInstruction>(insertIndex).let { instruction ->
                        Pair(instruction.registerA, instruction.registerB)
                    }

                engagementPanelBuilderMethod = this
                engagementPanelIdRegister = freeRegister
                engagementPanelFreeRegister =
                    findFreeRegister(insertIndex, freeRegister, panelRegister)
                engagementPanelIdInstruction =
                    "iget-object v$engagementPanelIdRegister, v$panelRegister, $panelIdField"
                engagementPanelIdIndex = insertIndex + 1

                addInstructions(
                    insertIndex,
                    """
                        $engagementPanelIdInstruction
                        invoke-static {v$engagementPanelIdRegister}, $EXTENSION_CLASS_DESCRIPTOR->setId(Ljava/lang/String;)V
                    """
                )
            }
        }

        engagementPanelUpdateFingerprint.method.addInstruction(
            0,
            "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hide()V"
        )
    }
}

internal fun addEngagementPanelIdHook(descriptor: String) =
    engagementPanelBuilderMethod.addInstructionsWithLabels(
        engagementPanelIdIndex, """
            $engagementPanelIdInstruction
            invoke-static {v$engagementPanelIdRegister}, $descriptor
            move-result v$engagementPanelFreeRegister
            if-eqz v$engagementPanelFreeRegister, :shown
            const/4 v$engagementPanelFreeRegister, 0x0
            return-object v$engagementPanelFreeRegister
            :shown
            nop
            """
    )
