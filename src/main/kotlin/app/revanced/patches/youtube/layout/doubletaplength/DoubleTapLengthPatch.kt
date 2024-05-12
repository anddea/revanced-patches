package app.revanced.patches.youtube.layout.doubletaplength

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addEntryValues
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.patch.BaseResourcePatch
import java.nio.file.Files

@Suppress("DEPRECATION", "unused")
object DoubleTapLengthPatch : BaseResourcePatch(
    name = "Custom double tap length",
    description = "Add 'double-tap to seek' value.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private val DoubleTapLengthArrays by stringPatchOption(
        key = "DoubleTapLengthArrays",
        default = "3, 5, 10, 15, 20, 30, 60, 120, 180",
        title = "Double-tap to seek Values",
        description = "A list of custom double-tap to seek lengths. Be sure to separate them with commas (,).",
        required = true
    )

    override fun execute(context: ResourceContext) {
        val arrayPath = "res/values-v21/arrays.xml"
        val entriesName = "double_tap_length_entries"
        val entryValueName = "double_tap_length_values"

        val valuesV21Directory = context["res"].resolve("values-v21")
        if (!valuesV21Directory.isDirectory)
            Files.createDirectories(valuesV21Directory.toPath())

        /**
         * Copy arrays
         */
        context.copyResources(
            "youtube/doubletap",
            ResourceGroup(
                "values-v21",
                "arrays.xml"
            )
        )

        val length = DoubleTapLengthArrays
            ?: throw PatchException("Invalid double-tap length array.")

        val splits = length.replace(" ", "").split(",")
        if (splits.isEmpty()) throw IllegalArgumentException("Invalid double-tap length elements")
        val lengthElements = splits.map { it }
        for (index in 0 until splits.count()) {
            context.addEntryValues(arrayPath, lengthElements[index], entryValueName)
            context.addEntryValues(arrayPath, lengthElements[index], entriesName)
        }

        SettingsPatch.updatePatchStatus(this)
    }
}