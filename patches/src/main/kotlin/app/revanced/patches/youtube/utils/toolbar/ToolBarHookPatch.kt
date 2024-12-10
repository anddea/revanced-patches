package app.revanced.patches.youtube.utils.toolbar

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/ToolBarPatch;"

private lateinit var toolbarMethod: MutableMethod

val toolBarHookPatch = bytecodePatch(
    description = "toolBarHookPatch"
) {
    dependsOn(sharedResourceIdPatch)

    execute {
        toolBarButtonFingerprint.matchOrThrow().let {
            it.method.apply {
                val replaceIndex = it.patternMatch!!.startIndex
                val freeIndex = it.patternMatch!!.endIndex - 1

                val replaceReference = getInstruction<ReferenceInstruction>(replaceIndex).reference
                val replaceRegister =
                    getInstruction<FiveRegisterInstruction>(replaceIndex).registerC
                val enumRegister = getInstruction<FiveRegisterInstruction>(replaceIndex).registerD
                val freeRegister = getInstruction<TwoRegisterInstruction>(freeIndex).registerA

                val imageViewIndex = replaceIndex + 2
                val imageViewReference =
                    getInstruction<ReferenceInstruction>(imageViewIndex).reference

                addInstructions(
                    replaceIndex + 1, """
                        iget-object v$freeRegister, p0, $imageViewReference
                        invoke-static {v$enumRegister, v$freeRegister}, $EXTENSION_CLASS_DESCRIPTOR->hookToolBar(Ljava/lang/Enum;Landroid/widget/ImageView;)V
                        invoke-interface {v$replaceRegister, v$enumRegister}, $replaceReference
                        """
                )
                removeInstruction(replaceIndex)
            }
        }

        toolbarMethod = toolBarPatchFingerprint.methodOrThrow()
    }
}

internal fun hookToolBar(descriptor: String) =
    toolbarMethod.addInstructions(
        0,
        "invoke-static {p0, p1}, $descriptor(Ljava/lang/String;Landroid/view/View;)V"
    )
