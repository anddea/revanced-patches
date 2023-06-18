package app.revanced.patches.youtube.layout.seekbar.timestamps.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.seekbar.timestamps.fingerprints.TimeCounterFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR

@Patch
@Name("hide-time-stamp")
@Description("Hides timestamp in video player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HideTimeStampPatch : BytecodePatch(
    listOf(TimeCounterFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        TimeCounterFingerprint.result?.mutableMethod?.let {
            it.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $SEEKBAR->hideTimeStamp()Z
                    move-result v0
                    if-eqz v0, :show_time_stamp
                    return-void
                    """, ExternalLabel("show_time_stamp", it.getInstruction(0))
            )
        } ?: return TimeCounterFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: HIDE_TIME_STAMP"
            )
        )

        SettingsPatch.updatePatchStatus("hide-time-stamp")

        return PatchResultSuccess()
    }
}
