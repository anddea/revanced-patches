package app.morphe.patches.youtube.layout.header

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.CUSTOM_HEADER_FOR_YOUTUBE
import app.morphe.patches.youtube.utils.settings.ResourceUtils.getIconType
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.Utils.printWarn
import app.morphe.util.Utils.trimIndentMultiline
import app.morphe.util.copyFile
import app.morphe.util.copyResources
import app.morphe.util.underBarOrThrow
import app.morphe.util.valueOrThrow
import java.io.File
import java.nio.file.Files
import kotlin.io.path.copyTo
import kotlin.io.path.exists

private const val GENERIC_HEADER_FILE_NAME = "yt_wordmark_header"
private const val PREMIUM_HEADER_FILE_NAME = "yt_premium_wordmark_header"

private const val NEW_GENERIC_HEADER_FILE_NAME = "yt_ringo2_wordmark_header"
private const val NEW_PREMIUM_HEADER_FILE_NAME = "yt_ringo2_premium_wordmark_header"

private const val DEFAULT_HEADER_KEY = "Custom branding icon"
private const val DEFAULT_HEADER_VALUE = "custom_branding_icon"

private val genericHeaderResourceDirectoryNames = mapOf(
    "xxxhdpi" to "488px x 192px",
    "xxhdpi" to "366px x 144px",
    "xhdpi" to "244px x 96px",
    "hdpi" to "184px x 72px",
    "mdpi" to "122px x 48px",
).map { (dpi, dim) ->
    "drawable-$dpi" to dim
}.toMap()

private val premiumHeaderResourceDirectoryNames = mapOf(
    "xxxhdpi" to "516px x 192px",
    "xxhdpi" to "387px x 144px",
    "xhdpi" to "258px x 96px",
    "hdpi" to "194px x 72px",
    "mdpi" to "129px x 48px",
).map { (dpi, dim) ->
    "drawable-$dpi" to dim
}.toMap()

private val variants = arrayOf("light", "dark")

private val headerIconResourceGroups =
    premiumHeaderResourceDirectoryNames.keys.map { directory ->
        ResourceGroup(
            directory,
            *variants.map { variant -> "${GENERIC_HEADER_FILE_NAME}_$variant.png" }
                .toTypedArray(),
            *variants.map { variant -> "${PREMIUM_HEADER_FILE_NAME}_$variant.png" }
                .toTypedArray(),
        )
    }

@Suppress("unused")
val changeHeaderPatch = resourcePatch(
    CUSTOM_HEADER_FOR_YOUTUBE.title,
    CUSTOM_HEADER_FOR_YOUTUBE.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val customHeaderOption = stringOption(
        key = "customHeader",
        default = DEFAULT_HEADER_VALUE,
        values = mapOf(
            DEFAULT_HEADER_KEY to DEFAULT_HEADER_VALUE
        ),
        title = "Custom header",
        description = """
            The header to apply to the app.
            
            Patch option '$DEFAULT_HEADER_KEY' applies only when:

            1. Patch 'Custom branding icon for YouTube' is included.
            2. Patch option for 'Custom branding icon for YouTube' is selected from the preset.
            
            If a path to a folder is provided, the folder must contain one or more of the following folders, depending on the DPI of the device:

            ${premiumHeaderResourceDirectoryNames.keys.joinToString("\n") { "- $it" }}
            
            Each of the folders must contain all of the following files:

            [Generic header]

            ${variants.joinToString("\n") { variant -> "- ${GENERIC_HEADER_FILE_NAME}_$variant.png" }}

            The image dimensions must be as follows:
            
            ${
            genericHeaderResourceDirectoryNames.map { (dpi, dim) -> "- $dpi: $dim" }
                .joinToString("\n")
        }

            [Premium header]

            ${variants.joinToString("\n") { variant -> "- ${PREMIUM_HEADER_FILE_NAME}_$variant.png" }}

            The image dimensions must be as follows:
            ${
            premiumHeaderResourceDirectoryNames.map { (dpi, dim) -> "- $dpi: $dim" }
                .joinToString("\n")
        }
        """.trimIndentMultiline(),
        required = true,
    )

    execute {
        // Check patch options first.
        var customHeader = customHeaderOption
            .underBarOrThrow()

        val isPath = customHeader != DEFAULT_HEADER_VALUE
        val customBrandingIconType = getIconType()
        val customBrandingIconIncluded =
            customBrandingIconType != "default" && customBrandingIconType != "custom"
        customHeader = customHeaderOption.valueOrThrow()

        val warnings = "Invalid header path: $customHeader. Does not apply patches."

        if (isPath) {
            copyFile(
                headerIconResourceGroups,
                customHeader,
                warnings
            )
        } else if (customBrandingIconIncluded) {
            headerIconResourceGroups.let { resourceGroups ->
                resourceGroups.forEach {
                    copyResources("youtube/branding/$customBrandingIconType/header", it)
                }
            }
        } else {
            printWarn(warnings)
            return@execute
        }

        // The size of the new header is the same, only the file name is different.
        // So if custom headers were used the patch will copy them to the new headers.
        mapOf(
            PREMIUM_HEADER_FILE_NAME to NEW_PREMIUM_HEADER_FILE_NAME,
            GENERIC_HEADER_FILE_NAME to NEW_GENERIC_HEADER_FILE_NAME
        ).forEach { (original, replacement) ->
            premiumHeaderResourceDirectoryNames.keys.forEach {
                get("res").resolve(it).takeIf(File::exists)?.toPath()?.let { path ->
                    variants.forEach { mode ->
                        val newHeaderPath = path.resolve("${replacement}_$mode.webp")

                        if (newHeaderPath.exists()) {
                            val fromPath = path.resolve("${original}_$mode.png")
                            val toPath = path.resolve("${replacement}_$mode.png")

                            fromPath.copyTo(toPath, true)

                            // If the original file is in webp file format, a compilation error will occur.
                            // Remove it to prevent compilation errors.
                            Files.delete(newHeaderPath)
                        }
                    }
                }
            }
        }

    }
}
