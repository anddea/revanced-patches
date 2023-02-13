package app.revanced.patches.youtube.layout.general.tabletminiplayer.resource.patch

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
import app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.patch.TabletMiniPlayerBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper

@Patch
@Name("enable-tablet-miniplayer")
@Description("Enables the tablet mini player layout.")
@DependsOn(
    [
        TabletMiniPlayerBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class TabletMiniPlayerPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings2(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: LAYOUT_SETTINGS",
            "PREFERENCE_HEADER: GENERAL",
            "SETTINGS: ENABLE_TABLET_MINIPLAYER"
        )

        ResourceHelper.patchSuccess(
            context,
            "enable-tablet-miniplayer"
        )

        return PatchResultSuccess()
    }
}