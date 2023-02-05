package app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.patch.HideFullscreenPanelsBytecodePatch
import app.revanced.patches.youtube.layout.fullscreen.fullscreenpanels.bytecode.patch.FullscreenButtonContainerBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("hide-fullscreen-panels")
@Description("Hides video description and comments panel in fullscreen view.")
@DependsOn(
    [
        FullscreenButtonContainerBytecodePatch::class,
        HideFullscreenPanelsBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideFullscreenPanelsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings2(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: FULLSCREEN",
            "SETTINGS: HIDE_FULLSCREEN_PANELS"
        )

        ResourceHelper.patchSuccess(
            context,
            "hide-fullscreen-panels"
        )

        return PatchResultSuccess()
    }
}