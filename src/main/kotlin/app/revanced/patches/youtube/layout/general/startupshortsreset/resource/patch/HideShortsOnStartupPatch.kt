package app.revanced.patches.youtube.layout.general.startupshortsreset.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.layout.general.startupshortsreset.bytecode.patch.HideShortsOnStartupBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper

@Patch
@Name("hide-startup-shorts-player")
@Description("Disables playing YouTube Shorts when launching YouTube.")
@DependsOn(
    [
        HideShortsOnStartupBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideShortsOnStartupPatch : ResourcePatch {
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
            "SETTINGS: SHORTS_COMPONENT_PARENT.B",
            "SETTINGS: HIDE_STARTUP_SHORTS_PLAYER"
        )

        ResourceHelper.patchSuccess(
            context,
            "hide-startup-shorts-player"
        )

        return PatchResultSuccess()
    }
}