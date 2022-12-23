package app.revanced.patches.youtube.layout.fullscreen.fullscreenbuttoncontainer.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.layout.fullscreen.fullscreenbuttoncontainer.bytecode.patch.FullscreenButtonContainerBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("hide-fullscreen-buttoncontainer")
@Description("Hides the button containers in fullscreen.")
@DependsOn(
    [
        FullscreenButtonContainerBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class FullscreenButtonContainerPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings2(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: FULLSCREEN",
            "SETTINGS: HIDE_FULLSCREEN_BUTTON_CONTAINER"
        )

        ResourceHelper.patchSuccess(
            context,
            "hide-fullscreen-buttoncontainer"
        )

        return PatchResultSuccess()
    }
}