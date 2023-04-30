package app.revanced.patches.youtube.layout.etc.doubletaplength.patch

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
import app.revanced.util.resources.ResourceHelper.addEntryValues
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import app.revanced.util.resources.ResourceUtils.copyXmlNode

@Patch
@Name("custom-double-tap-length")
@Description("Add 'double-tap to seek' value.")
@DependsOn(
    [
        PatchOptions::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class DoubleTapLengthPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /**
         * Copy arrays
         */
        context.copyResources(
            "youtube/doubletap",
            ResourceUtils.ResourceGroup(
                "values-v21",
                "arrays.xml"
            )
        )

        val speed = PatchOptions.CustomDoubleTapLengthArrays
            ?: return PatchResultError("Invalid double-tap length array.")

        val splits = speed.replace(" ","").split(",")
        if (splits.isEmpty()) throw IllegalArgumentException("Invalid speed elements")
        val speedElements = splits.map { it }
        for (index in 0 until splits.count()) {
            context.addEntryValues(TARGET_ARRAY_PATH, speedElements[index], TARGET_ENTRY_VALUE_NAME)
            context.addEntryValues(TARGET_ARRAY_PATH, speedElements[index], TARGET_ENTRIES_NAME)
        }

        SettingsPatch.updatePatchStatus("custom-double-tap-length")

        return PatchResultSuccess()
    }

    private companion object {
        private const val TARGET_ARRAY_PATH = "res/values-v21/arrays.xml"
        private const val TARGET_ENTRIES_NAME = "double_tap_length_entries"
        private const val TARGET_ENTRY_VALUE_NAME = "double_tap_length_values"
    }
}