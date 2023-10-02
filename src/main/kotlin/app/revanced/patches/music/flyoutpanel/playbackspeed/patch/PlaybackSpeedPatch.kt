package app.revanced.patches.music.flyoutpanel.playbackspeed.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.flyoutbutton.patch.FlyoutButtonContainerPatch
import app.revanced.patches.music.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType

@Patch
@Name("Enable playback speed")
@Description("Add playback speed button to the flyout panel.")
@DependsOn(
    [
        FlyoutButtonContainerPatch::class,
        OverrideSpeedHookPatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
class PlaybackSpeedPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.FLYOUT,
            "revanced_enable_flyout_panel_playback_speed",
            "false"
        )

    }
}
