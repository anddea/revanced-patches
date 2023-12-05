package app.revanced.patches.music.actionbar.downloadbuttonhook

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.actionbarhook.ActionBarHookPatch
import app.revanced.patches.music.utils.intenthook.IntentHookPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.video.information.VideoInformationPatch

@Patch(
    name = "Hook download button",
    description = "Replaces the offline download button with an external download button.",
    dependencies = [
        ActionBarHookPatch::class,
        IntentHookPatch::class,
        SettingsPatch::class,
        VideoInformationPatch::class
    ],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object DownloadButtonHookPatch : BytecodePatch(emptySet()) {
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
