package app.morphe.patches.youtube.utils.toolbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.indexOfGetDrawableInstruction
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.toolBarButtonFingerprint
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/ToolBarPatch;"

private lateinit var toolbarMethod: MutableMethod

val toolBarHookPatch = bytecodePatch(
    description = "toolBarHookPatch"
) {
    dependsOn(sharedResourceIdPatch)

    execute {
        toolBarButtonFingerprint.methodOrThrow().apply {
            val getDrawableIndex = indexOfGetDrawableInstruction(this)
            val enumOrdinalIndex = indexOfFirstInstructionReversedOrThrow(getDrawableIndex) {
                opcode == Opcode.INVOKE_INTERFACE &&
                        getReference<MethodReference>()?.returnType == "I"
            }
            val freeIndex = getDrawableIndex - 1

            val replaceReference = getInstruction<ReferenceInstruction>(enumOrdinalIndex).reference
            val replaceRegister =
                getInstruction<FiveRegisterInstruction>(enumOrdinalIndex).registerC
            val enumRegister = getInstruction<FiveRegisterInstruction>(enumOrdinalIndex).registerD
            val freeRegister = getInstruction<TwoRegisterInstruction>(freeIndex).registerA

            val imageViewIndex = indexOfFirstInstructionOrThrow(enumOrdinalIndex) {
                opcode == Opcode.IGET_OBJECT &&
                        getReference<FieldReference>()?.type == "Landroid/widget/ImageView;"
            }
            val imageViewReference =
                getInstruction<ReferenceInstruction>(imageViewIndex).reference

            addInstructions(
                enumOrdinalIndex + 1, """
                    iget-object v$freeRegister, p0, $imageViewReference
                    invoke-static {v$enumRegister, v$freeRegister}, $EXTENSION_CLASS_DESCRIPTOR->hookToolBar(Ljava/lang/Enum;Landroid/widget/ImageView;)V
                    invoke-interface {v$replaceRegister, v$enumRegister}, $replaceReference
                    """
            )
            removeInstruction(enumOrdinalIndex)
        }

        toolbarMethod = toolBarPatchFingerprint.methodOrThrow()
    }
}

internal fun hookToolBar(descriptor: String) =
    toolbarMethod.addInstructions(
        0,
        "invoke-static {p0, p1}, $descriptor(Ljava/lang/String;Landroid/view/View;)V"
    )
