package app.revanced.patches.music.general.autocaptions

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.videoid.VideoIdPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object AutoCaptionsPatch : BaseResourcePatch(
    name = "Disable auto captions",
    description = "Adds an option to disable captions from being automatically enabled.",
    dependencies = setOf(
        AutoCaptionsBytecodePatch::class,
        SettingsPatch::class,
        VideoIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: ResourceContext) {

        VideoIdPatch.hookVideoId("$GENERAL_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;)V")

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_captions",
            "false"
        )

    }
}