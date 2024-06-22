package app.revanced.patches.shared.ads

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.ads.fingerprints.MusicAdsFingerprint
import app.revanced.patches.shared.ads.fingerprints.VideoAdsFingerprint
import app.revanced.patches.shared.integrations.Constants.PATCHES_PATH
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getTargetIndexWithMethodReferenceNameOrThrow
import app.revanced.util.getWalkerMethod
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

abstract class BaseAdsPatch(
    private val classDescriptor: String,
    private val methodDescriptor: String
) : BytecodePatch(
    setOf(
        MusicAdsFingerprint,
        VideoAdsFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {
        MusicAdsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = indexOfFirstInstructionOrThrow {
                    val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                    opcode == Opcode.INVOKE_VIRTUAL
                            && reference?.returnType == "V"
                            && reference.parameterTypes.size == 1
                            && reference.parameterTypes.first() == "Z"
                }

                getWalkerMethod(context, targetIndex)
                    .addInstructions(
                        0, """
                            invoke-static {p1}, $classDescriptor->$methodDescriptor(Z)Z
                            move-result p1
                            """
                    )
            }
        }

        VideoAdsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $classDescriptor->$methodDescriptor()Z
                        move-result v0
                        if-nez v0, :show_ads
                        return-void
                        """, ExternalLabel("show_ads", getInstruction(0))
                )
            }
        }
    }

    internal fun MethodFingerprintResult.hookNonLithoFullscreenAds(literal: Long) {
        mutableMethod.apply {
            val targetIndex = getWideLiteralInstructionIndex(literal) + 2
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->hideFullscreenAds(Landroid/view/View;)V"
            )
        }
    }

    internal fun MethodFingerprintResult.hookLithoFullscreenAds(context: BytecodeContext) {
        mutableMethod.apply {
            val dialogCodeIndex = scanResult.patternScanResult!!.endIndex
            val dialogCodeField =
                getInstruction<ReferenceInstruction>(dialogCodeIndex).reference as FieldReference
            if (dialogCodeField.type != "I")
                throw PatchException("Invalid dialogCodeField: $dialogCodeField")

            // Disable fullscreen ads
            addInstructionsWithLabels(
                0,
                """
                        move-object/from16 v0, p2

                        # In the latest version of YouTube and YouTube Music, it is used after being cast

                        check-cast v0, ${dialogCodeField.definingClass}
                        iget v0, v0, $dialogCodeField
                        invoke-static {v0}, $INTEGRATIONS_CLASS_DESCRIPTOR->disableFullscreenAds(I)Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
            )

            // Close fullscreen ads

            // Find the instruction whose name is "show" in [MethodReference] and click the 'AlertDialog.BUTTON_POSITIVE' button.
            // In this case, an instruction for 'getButton' must be added to smali, not in integrations
            // (This custom dialog cannot be cast to [AlertDialog] or [Dialog])
            val dialogIndex = getTargetIndexWithMethodReferenceNameOrThrow("show")
            val dialogReference = getInstruction<ReferenceInstruction>(dialogIndex).reference
            val dialogDefiningClass = (dialogReference as MethodReference).definingClass
            val getButtonMethod = context.findClass(dialogDefiningClass)!!
                .mutableClass.methods.first { method ->
                    method.parameters == listOf("I")
                            && method.returnType == "Landroid/widget/Button;"
                }
            val getButtonCall =
                dialogDefiningClass + "->" + getButtonMethod.name + "(I)Landroid/widget/Button;"
            val dialogRegister = getInstruction<FiveRegisterInstruction>(dialogIndex).registerC
            val freeIndex = getTargetIndexOrThrow(dialogIndex, Opcode.IF_EQZ)
            val freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

            addInstructions(
                dialogIndex + 1, """
                    # Get the 'AlertDialog.BUTTON_POSITIVE' from custom dialog
                    # Since this custom dialog cannot be cast to AlertDialog or Dialog,
                    # It should come from smali, not integrations.
                    const/4 v$freeRegister, -0x1
                    invoke-virtual {v$dialogRegister, v$freeRegister}, $getButtonCall
                    move-result-object v$freeRegister
                    invoke-static {v$freeRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setCloseButton(Landroid/widget/Button;)V
                    """
            )
        }
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$PATCHES_PATH/FullscreenAdsPatch;"
    }
}