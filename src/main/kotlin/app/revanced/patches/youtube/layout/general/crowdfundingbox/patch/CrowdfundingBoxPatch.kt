package app.revanced.patches.youtube.layout.general.crowdfundingbox.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.crowdfundingbox.fingerprints.CrowdfundingBoxFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch
@Name("hide-crowdfunding-box")
@Description("Hides the crowdfunding box between the player and video description.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourcdIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class CrowdfundingBoxPatch : BytecodePatch(
    listOf(
        CrowdfundingBoxFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        CrowdfundingBoxFingerprint.result?.let {
            with (it.mutableMethod) {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = (instruction(insertIndex) as TwoRegisterInstruction).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$register}, $GENERAL_LAYOUT->hideCrowdfundingBox(Landroid/view/View;)V"
                )
            }
        } ?: return CrowdfundingBoxFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_LAYOUT_SETTINGS",
                "SETTINGS: HIDE_CROWDFUNDING_BOX"
            )
        )

        SettingsPatch.updatePatchStatus("hide-crowdfunding-box")

        return PatchResultSuccess()
    }
}
