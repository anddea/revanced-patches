package app.revanced.patches.youtube.utils.toolbar

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.toolbar.fingerprints.ToolBarButtonFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object ToolBarHookPatch : BytecodePatch(
    setOf(ToolBarButtonFingerprint)
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$UTILS_PATH/ToolBarPatch;"

    override fun execute(context: BytecodeContext) {

        ToolBarButtonFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val endIndex = it.scanResult.patternScanResult!!.endIndex

                val insertIndex = endIndex - 1
                val enumRegister = getInstruction<OneRegisterInstruction>(startIndex).registerA
                val freeRegister = getInstruction<TwoRegisterInstruction>(endIndex).registerA

                val imageViewReference = getInstruction<ReferenceInstruction>(insertIndex).reference

                addInstructions(
                    insertIndex, """
                        iget-object v$freeRegister, p0, $imageViewReference
                        invoke-static {v$enumRegister, v$freeRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->hookToolBar(Ljava/lang/Enum;Landroid/widget/ImageView;)V
                        """
                )
            }
        } ?: throw ToolBarButtonFingerprint.exception
    }
}