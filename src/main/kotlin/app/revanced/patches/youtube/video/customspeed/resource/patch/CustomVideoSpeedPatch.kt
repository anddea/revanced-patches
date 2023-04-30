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
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.options.PatchOptions
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.video.customspeed.bytecode.patch.CustomVideoSpeedBytecodePatch
import app.revanced.util.resources.ResourceHelper.addEntries
import app.revanced.util.resources.ResourceHelper.addEntryValues
import app.revanced.util.resources.ResourceUtils.copyXmlNode

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
        context.copyXmlNode("youtube/customspeed/host", "values/arrays.xml", "resources")

        val speed = PatchOptions.CustomSpeedArrays
            ?: return PatchResultError("Invalid video speed array.")

        val splits = speed.replace(" ","").split(",")
        if (splits.isEmpty()) throw IllegalArgumentException("Invalid speed elements")
        val speedElements = splits.map { it }
        for (index in 0 until splits.count()) {
            context.addEntries(TARGET_ARRAY_PATH, speedElements[index] + "x", TARGET_ENTRIES_NAME)
            context.addEntryValues(TARGET_ARRAY_PATH, speedElements[index], TARGET_ENTRY_VALUE_NAME)
        }

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: CUSTOM_VIDEO_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("custom-video-speed")

        return PatchResultSuccess()
    }
    private companion object {
        private const val TARGET_ARRAY_PATH = "res/values/arrays.xml"
        private const val TARGET_ENTRIES_NAME = "revanced_custom_video_speed_entry"
        private const val TARGET_ENTRY_VALUE_NAME = "revanced_custom_video_speed_entry_value"
    }
}