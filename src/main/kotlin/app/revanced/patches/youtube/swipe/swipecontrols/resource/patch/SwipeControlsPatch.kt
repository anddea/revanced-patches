package app.revanced.patches.youtube.swipe.swipecontrols.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.swipe.hdrbrightness.patch.HDRBrightnessPatch
import app.revanced.patches.youtube.swipe.swipecontrols.bytecode.patch.SwipeControlsBytecodePatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources

@Patch
@Name("Swipe controls")
@Description("Adds volume and brightness swipe controls.")
@DependsOn(
    [
        HDRBrightnessPatch::class,
        SettingsPatch::class,
        SwipeControlsBytecodePatch::class
    ]
)
@YouTubeCompatibility
class SwipeControlsPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SWIPE_SETTINGS",
                "SETTINGS: SWIPE_CONTROLS"
            )
        )

        SettingsPatch.updatePatchStatus("swipe-controls")

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
    }
}
