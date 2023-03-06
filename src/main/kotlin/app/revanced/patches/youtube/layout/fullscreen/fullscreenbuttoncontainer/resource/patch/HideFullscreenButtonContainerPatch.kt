package app.revanced.patches.youtube.layout.fullscreen.fullscreenbuttoncontainer.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.fullscreen.fullscreenbuttoncontainer.bytecode.patch.HideFullscreenButtonContainerBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-fullscreen-buttoncontainer")
@Description("Hides the button containers in fullscreen.")
@DependsOn(
    [
        HideFullscreenButtonContainerBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideFullscreenButtonContainerPatch : ResourcePatch {

    override fun execute(context: ResourceContext): PatchResult {

        /*
         * Add ReVanced Settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_LAYOUT_SETTINGS",
                "SETTINGS: HIDE_FULLSCREEN_BUTTON_CONTAINER"
            )
        )

        SettingsPatch.updatePatchStatus("hide-fullscreen-buttoncontainer")

        return PatchResultSuccess()
    }
}