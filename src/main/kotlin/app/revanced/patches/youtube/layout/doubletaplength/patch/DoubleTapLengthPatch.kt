package app.revanced.patches.youtube.layout.doubletaplength.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper.addEntryValues
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources

@Patch
@Name("custom-double-tap-length")
@Description("Add 'double-tap to seek' value.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class DoubleTapLengthPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        val arrayPath = "res/values-v21/arrays.xml"
        val entriesName = "double_tap_length_entries"
        val entryValueName = "double_tap_length_values"

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

        val length = DoubleTapLengthArrays
            ?: return PatchResultError("Invalid double-tap length array.")

        val splits = length.replace(" ","").split(",")
        if (splits.isEmpty()) throw IllegalArgumentException("Invalid double-tap length elements")
        val lengthElements = splits.map { it }
        for (index in 0 until splits.count()) {
            context.addEntryValues(arrayPath, lengthElements[index], entryValueName)
            context.addEntryValues(arrayPath, lengthElements[index], entriesName)
        }

        SettingsPatch.updatePatchStatus("custom-double-tap-length")

        return PatchResultSuccess()
    }

    companion object : OptionsContainer() {
        var DoubleTapLengthArrays: String? by option(
            PatchOption.StringOption(
                key = "DoubleTapLengthArrays",
                default = "3, 5, 10, 15, 20, 30, 60, 120, 180",
                title = "Double-tap to seek Values",
                description = "A list of custom double-tap to seek lengths. Be sure to separate them with commas (,)."
            )
        )
    }
}