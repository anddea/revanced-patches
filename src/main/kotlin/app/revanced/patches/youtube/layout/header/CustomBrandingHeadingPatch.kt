package app.revanced.patches.youtube.layout.header

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch
import java.io.File

@Suppress("DEPRECATION", "unused")
object CustomBrandingHeadingPatch : BaseResourcePatch(
    name = "Force custom branding wordmark header",
    description = "Applies a custom heading in the top left corner within the app.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    private const val DEFAULT_HEADING_NAME = "yt_wordmark_header"
    private const val PREMIUM_HEADING_NAME = "yt_premium_wordmark_header"

    private val availableHeading = mapOf(
        "YouTube" to DEFAULT_HEADING_NAME,
        "YouTube Premium" to PREMIUM_HEADING_NAME,
    )

    private val targetResourceDirectoryNames = mapOf(
        "xxxhdpi" to "512px x 192px",
        "xxhdpi" to "387px x 144px",
        "xhdpi" to "258px x 96px",
        "hdpi" to "194px x 72px",
        "mdpi" to "129px x 48px",
    ).map { (dpi, dim) ->
        "drawable-$dpi" to dim
    }.toMap()

    private val variants = arrayOf("light", "dark")

    private val header by stringPatchOption(
        key = "header",
        default = PREMIUM_HEADING_NAME,
        values = availableHeading,
        title = "Header",
        description = """
            The header to apply to the app.
            
            If a path to a folder is provided, the folder must contain one or more of the following folders, depending on the DPI of the device:
            
            ${targetResourceDirectoryNames.keys.joinToString("\n") { "- $it" }}
            
            Each of the folders must contain all of the following files:
            
            ${variants.joinToString("\n") { variant -> "- ${DEFAULT_HEADING_NAME}_$variant.png" }}
            The image dimensions must be as follows:
            ${targetResourceDirectoryNames.map { (dpi, dim) -> "- $dpi: $dim" }.joinToString("\n")}
        """
        .split("\n")
        .joinToString("\n") { it.trimIndent() } // Remove the leading whitespace from each line.
        .trimIndent(), // Remove the leading newline.,
    )

    override fun execute(context: ResourceContext) {
        // The directories to copy the header to.
        val targetResourceDirectories = targetResourceDirectoryNames.keys.mapNotNull {
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
        }

        val toHeader = {
            overwriteFromTo(DEFAULT_HEADING_NAME, PREMIUM_HEADING_NAME)
        }

        val toCustom = {
            val sourceFolders = File(header!!).listFiles { file -> file.isDirectory }
                ?: throw PatchException("The provided path is not a directory: $header")

            var copiedFiles = false

            // For each source folder, copy the files to the target resource directories.
            sourceFolders.forEach { dpiSourceFolder ->
                val targetDpiFolder = context["res"].resolve(dpiSourceFolder.name)
                if (!targetDpiFolder.exists()) return@forEach

                val imgSourceFiles = dpiSourceFolder.listFiles { file -> file.isFile }!!
                imgSourceFiles.forEach { imgSourceFile ->
                    val imgTargetFile = targetDpiFolder.resolve(imgSourceFile.name)
                    imgSourceFile.copyTo(imgTargetFile, true)

                    copiedFiles = true
                }
            }

            if (!copiedFiles) {
                throw PatchException("No header files were copied from the provided path: $header.")
            }

            // Overwrite the premium with the custom header as well.
            toHeader()
        }

        when (header) {
            DEFAULT_HEADING_NAME -> toHeader
            PREMIUM_HEADING_NAME -> toPremium
            else -> toCustom
        }()
    }
}
