package app.revanced.patches.music.buttoncontainer.downloadbuttonhook.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.buttoncontainerhook.patch.ButtonContainerHookPatch
import app.revanced.patches.music.utils.intenthook.patch.IntentHookPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.music.utils.videoid.patch.VideoIdPatch
import app.revanced.util.enum.CategoryType

@Patch
@Name("Hook download button")
@Description("Replaces the offline download button in the button container with an external download button.")
@DependsOn(
    [
        ButtonContainerHookPatch::class,
        IntentHookPatch::class,
        SettingsPatch::class,
        VideoIdPatch::class
    ]
)
@MusicCompatibility
class DownloadButtonHookPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.BUTTON_CONTAINER,
            "revanced_hook_button_container_download",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.BUTTON_CONTAINER,
            "revanced_external_downloader_package_name",
            "revanced_hook_button_container_download"
        )

    }
}
