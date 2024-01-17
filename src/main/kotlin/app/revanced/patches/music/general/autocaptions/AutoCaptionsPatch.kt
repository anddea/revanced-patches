package app.revanced.patches.music.general.autocaptions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.integrations.Constants.GENERAL
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.videoid.VideoIdPatch
import app.revanced.patches.shared.patch.captions.AbstractAutoCaptionsPatch

@Patch(
    name = "Disable auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
    dependencies = [
        SettingsPatch::class,
        VideoIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ],
)
@Suppress("unused")
object AutoCaptionsPatch : AbstractAutoCaptionsPatch(
    GENERAL
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        VideoIdPatch.hookBackgroundPlayVideoId("$GENERAL->newVideoStarted(Ljava/lang/String;)V")

        SettingsPatch.addMusicPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_captions",
            "false"
        )

    }
}