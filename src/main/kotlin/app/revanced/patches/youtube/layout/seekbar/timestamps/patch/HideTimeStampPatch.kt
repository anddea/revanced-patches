package app.revanced.patches.youtube.layout.seekbar.timestamps.patch

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
import app.revanced.patches.youtube.layout.seekbar.timestamps.fingerprints.TimeStampsContainerFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR_LAYOUT

@Patch
@Name("hide-time-stamp")
@Description("Hides the time counter above the seekbar.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourcdIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideTimeStampPatch : BytecodePatch(
    listOf(
        TimeStampsContainerFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        TimeStampsContainerFingerprint.result?.mutableMethod?.let {
            it.addInstructions(
                0, """
                    invoke-static {}, $SEEKBAR_LAYOUT->hideTimeStamp()Z
                    move-result v0
                    if-eqz v0, :show_time_stamp
                    return-void
                    """, listOf(ExternalLabel("show_time_stamp", it.instruction(0)))
            )
        } ?: return TimeStampsContainerFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_LAYOUT_SETTINGS",
                "SETTINGS: HIDE_TIME_STAMP"
            )
        )

        SettingsPatch.updatePatchStatus("hide-time-stamp")

        return PatchResultSuccess()
    }
}
