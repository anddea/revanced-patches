package app.revanced.patches.youtube.general.latestvideosbutton.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.general.latestvideosbutton.fingerprints.LatestVideosButtonFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Hide latest videos button")
@Description("Hides latest videos button in home feed.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class LatestVideosButtonPatch : BytecodePatch(
    listOf(LatestVideosButtonFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        LatestVideosButtonFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $GENERAL->hideLatestVideosButton(Landroid/view/View;)V"
                )
            }
        } ?: return LatestVideosButtonFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_LATEST_VIDEOS_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-latest-videos-button")

        return PatchResultSuccess()
    }
}
