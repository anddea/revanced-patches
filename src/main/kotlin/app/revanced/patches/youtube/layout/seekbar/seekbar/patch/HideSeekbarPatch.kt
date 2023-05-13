package app.revanced.patches.youtube.layout.seekbar.seekbar.patch

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
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.misc.timebar.patch.HookTimeBarPatch
import app.revanced.util.integrations.Constants.SEEKBAR

@Patch
@Name("hide-seekbar")
@Description("Hides the seekbar and progressbar.")
@DependsOn(
    [
        HookTimeBarPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideSeekbarPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        val insertMethod = HookTimeBarPatch.setTimeBarMethod

        insertMethod.addInstructions(
            0, """
                invoke-static {}, $SEEKBAR->hideSeekbar()Z
                move-result v0
                if-eqz v0, :show_seekbar
                return-void
                """, listOf(ExternalLabel("show_seekbar", insertMethod.instruction(0)))
        )

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: HIDE_SEEKBAR"
            )
        )

        SettingsPatch.updatePatchStatus("hide-seekbar")

        return PatchResultSuccess()
    }
}
