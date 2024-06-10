package app.revanced.patches.youtube.ads.general

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.ads.general.VideoAdsPatch.hookLithoFullscreenAds
import app.revanced.patches.youtube.ads.general.VideoAdsPatch.hookNonLithoFullscreenAds
import app.revanced.patches.youtube.ads.general.fingerprints.CompactYpcOfferModuleViewFingerprint
import app.revanced.patches.youtube.ads.general.fingerprints.InterstitialsContainerFingerprint
import app.revanced.patches.youtube.ads.general.fingerprints.ShowDialogCommandFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.ADS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AdAttribution
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.InterstitialsContainer
import app.revanced.util.findMutableMethodOf
import app.revanced.util.injectHideViewCall
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch(dependencies = [SharedResourceIdPatch::class])
object AdsBytecodePatch : BytecodePatch(
    setOf(
        CompactYpcOfferModuleViewFingerprint,
        InterstitialsContainerFingerprint,
        ShowDialogCommandFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        // region patch for hide fullscreen ads

        // non-litho view, used in some old clients.
        InterstitialsContainerFingerprint
            .resultOrThrow()
            .hookNonLithoFullscreenAds(InterstitialsContainer)

        // litho view, used in 'ShowDialogCommandOuterClass' in innertube
        ShowDialogCommandFingerprint
            .resultOrThrow()
            .hookLithoFullscreenAds(context)

        // endregion

        // region patch for hide general ads

        hideAdAttributionView(context)

        // endregion

        // region patch for hide get premium

        CompactYpcOfferModuleViewFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val measuredWidthRegister =
                    getInstruction<TwoRegisterInstruction>(startIndex).registerA
                val measuredHeightInstruction =
                    getInstruction<TwoRegisterInstruction>(startIndex + 1)
                val measuredHeightRegister = measuredHeightInstruction.registerA
                val tempRegister = measuredHeightInstruction.registerB

                addInstructionsWithLabels(
                    startIndex + 2, """
                        invoke-static {}, $ADS_CLASS_DESCRIPTOR->hideGetPremium()Z
                        move-result v$tempRegister
                        if-eqz v$tempRegister, :show
                        const/4 v$measuredWidthRegister, 0x0
                        const/4 v$measuredHeightRegister, 0x0
                        """, ExternalLabel("show", getInstruction(startIndex + 2))
                )
            }
        }

        // endregion

    }

    private fun hideAdAttributionView(context: BytecodeContext) {
        context.classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                method.implementation.apply {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        if (instruction.opcode != Opcode.CONST)
                            return@forEachIndexed
                        // Instruction to store the id adAttribution into a register
                        if ((instruction as Instruction31i).wideLiteral != AdAttribution)
                            return@forEachIndexed

                        val insertIndex = index + 1

                        // Call to get the view with the id adAttribution
                        (instructions.elementAt(insertIndex)).apply {
                            if (opcode != Opcode.INVOKE_VIRTUAL)
                                return@forEachIndexed

                            // Hide the view
                            val viewRegister = (this as Instruction35c).registerC
                            context.proxy(classDef)
                                .mutableClass
                                .findMutableMethodOf(method)
                                .injectHideViewCall(
                                    insertIndex,
                                    viewRegister,
                                    ADS_CLASS_DESCRIPTOR,
                                    "hideAdAttributionView"
                                )
                        }
                    }
                }
            }
        }
    }
}
