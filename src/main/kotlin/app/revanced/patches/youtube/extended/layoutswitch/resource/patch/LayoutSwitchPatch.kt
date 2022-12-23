package app.revanced.patches.youtube.extended.layoutswitch.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.extended.layoutswitch.bytecode.patch.LayoutSwitchBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("layout-switch")
@Description("Tricks the dpi to use some tablet/phone layouts.")
@DependsOn(
    [
        LayoutSwitchBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class LayoutSwitchPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings2(
            context,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: EXTENDED_SETTINGS",
            "SETTINGS: EXPERIMENTAL_FLAGS",
            "SETTINGS: LAYOUT_SWITCH"
        )

        ResourceHelper.patchSuccess(
            context,
            "layout-switch"
        )

        return PatchResultSuccess()
    }
}