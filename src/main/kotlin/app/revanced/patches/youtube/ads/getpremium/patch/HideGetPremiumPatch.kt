package app.revanced.patches.youtube.ads.getpremium.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.ads.getpremium.fingerprints.CompactYpcOfferModuleViewFingerprint
import app.revanced.util.integrations.Constants.PATCHES_PATH
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Name("hide-get-premium")
@Version("0.0.1")
class HideGetPremiumPatch : BytecodePatch(
    listOf(CompactYpcOfferModuleViewFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        CompactYpcOfferModuleViewFingerprint.result?.let {
            it.mutableMethod.apply {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val measuredWidthRegister = instruction<TwoRegisterInstruction>(startIndex).registerA
                val measuredHeightInstruction = instruction<TwoRegisterInstruction>(startIndex + 1)
                val measuredHeightRegister = measuredHeightInstruction.registerA
                val tempRegister = measuredHeightInstruction.registerB

                addInstructions(
                    startIndex + 2, """
                        invoke-static {}, $PATCHES_PATH/ads/AdsFilter;->hideGetPremium()Z
                        move-result v$tempRegister
                        if-eqz v$tempRegister, :show
                        const/4 v$measuredWidthRegister, 0x0
                        const/4 v$measuredHeightRegister, 0x0
                        """, listOf(ExternalLabel("show", instruction(startIndex + 2)))
                )
            }
        } ?: return CompactYpcOfferModuleViewFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
