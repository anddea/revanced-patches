package app.revanced.patches.youtube.layout.general.getpremium.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.getpremium.fingerprints.CompactYpcOfferModuleViewFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-get-premium")
@Description("Hides the YouTube Premium promotion banner between the player and video description.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HideGetPremiumPatch : BytecodePatch(
    listOf(
        CompactYpcOfferModuleViewFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        CompactYpcOfferModuleViewFingerprint.result?.let {
            with (it.mutableMethod) {
                val startIndex = it.scanResult.patternScanResult!!.startIndex
                val measuredWidthRegister = (instruction(startIndex) as TwoRegisterInstruction).registerA
                val measuredHeightInstruction = instruction(startIndex + 1) as TwoRegisterInstruction
                val measuredHeightRegister = measuredHeightInstruction.registerA
                val tempRegister = measuredHeightInstruction.registerB

                addInstructions(
                    startIndex + 2, """
                        invoke-static {}, $GENERAL->hideGetPremium()Z
                        move-result v$tempRegister
                        if-eqz v$tempRegister, :show
                        const/4 v$measuredWidthRegister, 0x0
                        const/4 v$measuredHeightRegister, 0x0
                        """, listOf(ExternalLabel("show", instruction(startIndex + 2)))
                )
            }
        } ?: return CompactYpcOfferModuleViewFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_GET_PREMIUM"
            )
        )

        SettingsPatch.updatePatchStatus("hide-get-premium")

        return PatchResultSuccess()
    }
}
