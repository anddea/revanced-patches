package app.revanced.patches.music.video.customspeed

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.intenthook.IntentHookPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.patch.customspeed.AbstractCustomPlaybackSpeedPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_VIDEO_PATH

@Patch(
    name = "Custom playback speed",
    description = "Adds more playback speed options.",
    dependencies = [IntentHookPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.27.54",
                "6.28.52"
            ]
        )
    ],
)
@Suppress("unused")
object CustomPlaybackSpeedPatch : AbstractCustomPlaybackSpeedPatch(
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
