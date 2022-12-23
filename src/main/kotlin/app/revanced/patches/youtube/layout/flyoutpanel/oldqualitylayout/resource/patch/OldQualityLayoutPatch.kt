package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.bytecode.patch.OldQualityLayoutBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("enable-oldstyle-quality-layout")
@Description("Enables the original quality flyout menu.")
@DependsOn(
    [
        OldQualityLayoutBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class OldQualityLayoutPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings2(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: FLYOUT_PANEL",
            "SETTINGS: ENABLE_OLD_STYLE_QUALITY_LAYOUT"
        )

        ResourceHelper.patchSuccess(
            context,
            "enable-oldstyle-quality-layout"
        )

        return PatchResultSuccess()
    }
}