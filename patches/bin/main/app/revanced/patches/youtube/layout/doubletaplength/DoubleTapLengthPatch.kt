package app.revanced.patches.youtube.layout.doubletaplength

import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.CUSTOM_DOUBLE_TAP_LENGTH
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.addEntryValues
import app.revanced.util.copyResources
import app.revanced.util.valueOrThrow
import java.nio.file.Files

@Suppress("unused")
val doubleTapLengthPatch = resourcePatch(
    CUSTOM_DOUBLE_TAP_LENGTH.title,
    CUSTOM_DOUBLE_TAP_LENGTH.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val doubleTapLengthArraysOption = stringOption(
        key = "doubleTapLengthArrays",
        default = "3, 5, 10, 15, 20, 30, 60, 120, 180",
        title = "Double-tap to seek values",
        description = "A list of custom Double-tap to seek lengths to be added, separated by commas.",
        required = true,
    )

    execute {
        // Check patch options first.
        val doubleTapLengthArrays = doubleTapLengthArraysOption
            .valueOrThrow()

        // Check patch options first.
        val splits = doubleTapLengthArrays
            .replace(" ", "")
            .split(",")
        if (splits.isEmpty()) throw PatchException("Invalid double-tap length elements")
        val lengthElements = splits.map { it }

        val arrayPath = "res/values-v21/arrays.xml"
        val entriesName = "double_tap_length_entries"
        val entryValueName = "double_tap_length_values"

        val valuesV21Directory = get("res").resolve("values-v21")
        if (!valuesV21Directory.isDirectory)
            Files.createDirectories(valuesV21Directory.toPath())

        /**
         * Copy arrays
         */
        copyResources(
            "youtube/doubletap",
            ResourceGroup(
                "values-v21",
                "arrays.xml"
            )
        )

        for (index in 0 until splits.count()) {
            addEntryValues(
                entryValueName,
                lengthElements[index],
                path = arrayPath
            )
            addEntryValues(
                entriesName,
                lengthElements[index],
                path = arrayPath
            )
        }

        addPreference(CUSTOM_DOUBLE_TAP_LENGTH)

    }
}
