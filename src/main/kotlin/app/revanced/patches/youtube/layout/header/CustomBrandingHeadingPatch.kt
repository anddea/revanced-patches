package app.revanced.patches.youtube.layout.header

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusHeader
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import java.io.File

@Patch(
    name = "Custom branding heading",
    description = "Applies a custom heading in the top left corner within the app.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.34"
            ]
        )
    ],
)
@Suppress("unused")

object CustomBrandingHeadingPatch : ResourcePatch() {
    private const val DEFAULT_HEADING_NAME = "yt_wordmark_header"
    private const val PREMIUM_HEADING_NAME = "yt_premium_wordmark_header"

    private val availableHeading = mapOf(
        "YouTube" to DEFAULT_HEADING_NAME,
        "YouTube Premium" to PREMIUM_HEADING_NAME,
    )

    private val targetResourceDirectoryNames = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "mdpi",
        "hdpi",
    ).map { dpi ->
        "drawable-$dpi"
    }

    private val variants = arrayOf("light", "dark")

    private val header by stringPatchOption(
        key = "header",
        default = PREMIUM_HEADING_NAME,
        values = availableHeading,
        title = "Header",
        description = """
            The path to a folder must contain one or more of the following folders matching the DPI of your device:

            ${targetResourceDirectoryNames.joinToString("\n") { "- $it" }}

            Each of these folders has to have the following files:

            ${variants.joinToString("\n") { variant -> "- ${DEFAULT_HEADING_NAME}_$variant.png" }}
        """
        .split("\n")
        .joinToString("\n") { it.trimIndent() } // Remove the leading whitespace from each line.
        .trimIndent(), // Remove the leading newline.,
    )

    override fun execute(context: ResourceContext) {
        context.updatePatchStatusHeader("Default")

        // The directories to copy the header to.
        val targetResourceDirectories = targetResourceDirectoryNames.mapNotNull {
            context["res"].resolve(it).takeIf(File::exists)
        }

        /**
         * A function that overwrites both header variants from [from] to [to] in the target resource directories.
         */
        val overwriteFromTo: (String, String) -> Unit = { from: String, to: String ->
            targetResourceDirectories.forEach { directory ->
                variants.forEach { variant ->
                    val fromPath = directory.resolve("${from}_$variant.png")
                    val toPath = directory.resolve("${to}_$variant.png")

                    fromPath.copyTo(toPath, true)
                }
            }
        }

        // Functions to overwrite the header to the different variants.
        val toPremium = {
            overwriteFromTo(PREMIUM_HEADING_NAME, DEFAULT_HEADING_NAME)
            context.updatePatchStatusHeader("Premium")
        }

        val toHeader = {
            overwriteFromTo(DEFAULT_HEADING_NAME, PREMIUM_HEADING_NAME)
        }

        val toCustom = {
            var copiedReplacementImages = false
            // For all the resource groups in the custom header folder, copy them to the resource directories.
            File(header!!).listFiles { file -> file.isDirectory }?.forEach { folder ->
                val targetDirectory = context["res"].resolve(folder.name)
                // Skip if the target directory (DPI) doesn't exist.
                if (!targetDirectory.exists()) return@forEach

                folder.listFiles { file -> file.isFile }?.forEach {
                    val targetResourceFile = targetDirectory.resolve(it.name)

                    it.copyTo(targetResourceFile, true)
                    copiedReplacementImages = true
                }
            }

            if (!copiedReplacementImages) throw PatchException("Could not find any custom images resources in directory: $header")

            // Overwrite the premium with the custom header as well.
            toHeader()

            context.updatePatchStatusHeader("Custom")
        }

        when (header) {
            DEFAULT_HEADING_NAME -> toHeader
            PREMIUM_HEADING_NAME -> toPremium
            else -> toCustom
        }()
    }
}