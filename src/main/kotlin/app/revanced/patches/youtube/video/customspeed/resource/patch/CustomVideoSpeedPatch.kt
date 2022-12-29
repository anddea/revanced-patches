package app.revanced.patches.youtube.video.customspeed.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.video.customspeed.bytecode.patch.CustomVideoSpeedBytecodePatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.patches.options.PatchOptions
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils.copyXmlNode

@Patch
@Name("custom-video-speed")
@Description("Adds more video speed options.")
@DependsOn(
    [
        CustomVideoSpeedBytecodePatch::class,
        PatchOptions::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class CustomVideoSpeedPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         * Copy arrays
         */
        context.copyXmlNode("youtube/speed/host", "values/arrays.xml", "resources")

        val speed = PatchOptions.CustomSpeedArrays
            ?: return PatchResultError("Invalid video speed array.")

        val splits = speed.replace(" ","").split(",")
        if (splits.isEmpty()) throw IllegalArgumentException("Invalid speed elements")
        val speedElements = splits.map { it }
        for (index in 0 until splits.count()) {
            ResourceHelper.addEntryValues(context, speedElements[index])
            ResourceHelper.addEntries(context, speedElements[index] + "x")
        }

        ResourceHelper.addSpeed(context)

        /*
         add settings
         */
        ResourceHelper.addSettings(
            context,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: VIDEO_SETTINGS",
            "SETTINGS: CUSTOM_VIDEO_SPEED"
        )

        ResourceHelper.patchSuccess(
            context,
            "custom-video-speed"
        )

        return PatchResultSuccess()
    }
}