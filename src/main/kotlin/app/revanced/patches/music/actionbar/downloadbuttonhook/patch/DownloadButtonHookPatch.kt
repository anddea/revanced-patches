package app.revanced.patches.music.actionbar.downloadbuttonhook.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.actionbarhook.patch.ActionBarHookPatch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.intenthook.patch.IntentHookPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.music.video.information.patch.VideoInformationPatch
import app.revanced.util.enum.CategoryType

@Patch
@Name("Hook download button")
@Description("Replaces the offline download button with an external download button.")
@DependsOn(
    [
        ActionBarHookPatch::class,
        IntentHookPatch::class,
        SettingsPatch::class,
        VideoInformationPatch::class
    ]
)
@MusicCompatibility
class DownloadButtonHookPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hook_action_bar_download",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.ACTION_BAR,
            "revanced_external_downloader_package_name",
            "revanced_hook_action_bar_download"
        )

    }
}
