package app.revanced.patches.youtube.video.customspeed.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.patch.customspeed.AbstractCustomPlaybackSpeedPatch
import app.revanced.patches.youtube.flyoutpanel.oldspeedlayout.patch.OldSpeedLayoutPatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.VIDEO_PATH

@Patch
@Name("Custom playback speed")
@Description("Adds more playback speed options.")
@DependsOn(
    [
        OldSpeedLayoutPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class CustomPlaybackSpeedPatch : AbstractCustomPlaybackSpeedPatch(
    "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
    10.0f
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: CUSTOM_PLAYBACK_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("custom-playback-speed")
    }
}
