package app.revanced.patches.music.video.customspeed.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.intenthook.patch.IntentHookPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.shared.patch.customspeed.AbstractCustomPlaybackSpeedPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_VIDEO_PATH

@Patch
@Name("Custom playback speed")
@Description("Adds more playback speed options.")
@DependsOn([IntentHookPatch::class])
@MusicCompatibility
class CustomPlaybackSpeedPatch : AbstractCustomPlaybackSpeedPatch(
    "$MUSIC_VIDEO_PATH/CustomPlaybackSpeedPatch;",
    3.0f
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.VIDEO,
            "revanced_custom_playback_speeds"
        )

    }
}
