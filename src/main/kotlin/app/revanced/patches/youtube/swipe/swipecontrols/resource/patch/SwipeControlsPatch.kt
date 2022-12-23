package app.revanced.patches.youtube.swipe.swipecontrols.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.swipe.swipecontrols.bytecode.patch.SwipeControlsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils
import app.revanced.shared.util.resources.ResourceUtils.copyResources

@Patch
@Name("swipe-controls")
@Description("Adds volume and brightness swipe controls.")
@DependsOn(
    [
        SettingsPatch::class,
        SwipeControlsBytecodePatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class SwipeControlsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: SWIPE_SETTINGS",
            "SETTINGS: SWIPE_CONTROLS"
        )

        ResourceHelper.patchSuccess(
            context,
            "swipe-controls"
        )

        context.copyResources(
            "youtube/swipecontrols",
            ResourceUtils.ResourceGroup(
                "drawable",
                "ic_sc_brightness_auto.xml",
                "ic_sc_brightness_manual.xml",
                "ic_sc_volume_mute.xml",
                "ic_sc_volume_normal.xml"
            )
        )
        return PatchResultSuccess()
    }
}
