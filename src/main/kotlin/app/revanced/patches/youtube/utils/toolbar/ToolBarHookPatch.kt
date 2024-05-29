package app.revanced.patches.youtube.utils.toolbar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.toolbar.fingerprints.ToolBarButtonFingerprint
import app.revanced.patches.youtube.utils.toolbar.fingerprints.ToolBarPatchFingerprint
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object ToolBarHookPatch : BytecodePatch(
    setOf(
        ToolBarButtonFingerprint,
        ToolBarPatchFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$UTILS_PATH/ToolBarPatch;"

    private lateinit var toolbarMethod: MutableMethod

    override fun execute(context: BytecodeContext) {

        ToolBarButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val replaceIndex = it.scanResult.patternScanResult!!.startIndex
                val freeIndex = it.scanResult.patternScanResult!!.endIndex - 1

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
                        invoke-static {v$enumRegister, v$freeRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->hookToolBar(Ljava/lang/Enum;Landroid/widget/ImageView;)V
                        invoke-interface {v$replaceRegister, v$enumRegister}, $replaceReference
                        """
                )
                removeInstruction(replaceIndex)
            }
        }

        toolbarMethod = ToolBarPatchFingerprint.resultOrThrow().mutableMethod
    }

    internal fun hook(
        descriptor: String
    ) {
        toolbarMethod.addInstructions(
            0,
            "invoke-static {p0, p1}, $descriptor(Ljava/lang/String;Landroid/view/View;)V"
        )
    }
}