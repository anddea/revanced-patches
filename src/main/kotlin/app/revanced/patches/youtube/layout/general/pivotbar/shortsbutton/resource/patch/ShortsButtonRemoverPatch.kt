package app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.resource.patch

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
import app.revanced.patches.youtube.layout.general.pivotbar.shortsbutton.bytecode.patch.ShortsButtonRemoverBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper

@Patch
@Name("hide-shorts-button")
@Description("Hides the shorts button in the navigation bar.")
@DependsOn(
    [
        ShortsButtonRemoverBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ShortsButtonRemoverPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings4(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: SHORTS_COMPONENT.PARENT",
            "SETTINGS: SHORTS_COMPONENT_PARENT.A",
            "SETTINGS: HIDE_SHORTS_BUTTON"
        )

        ResourceHelper.patchSuccess(
            context,
            "hide-shorts-button"
        )

        return PatchResultSuccess()
    }
}