package app.revanced.patches.music.layout.header

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.compatibility.Constants
import app.revanced.patches.music.utils.fix.header.RestoreOldHeaderPatch
import app.revanced.patches.music.utils.settings.ResourceUtils
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.copyFile
import app.revanced.util.copyResources
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.underBarOrThrow

@Suppress("unused")
object ChangeHeaderPatch : BaseResourcePatch(
    name = "Custom header for YouTube Music",
    description = "Applies a custom header in the top left corner within the app.",
    dependencies = setOf(RestoreOldHeaderPatch::class),
    compatiblePackages = Constants.COMPATIBLE_PACKAGE,
    use = false,
) {
    private const val DEFAULT_HEADER_KEY = "Custom branding icon"
    private const val DEFAULT_HEADER_VALUE = "custom_branding_icon"

    private val actionBarLogoResourceDirectoryNames = mapOf(
        "xxxhdpi" to "320px x 96px",
        "xxhdpi" to "240px x 72px",
        "xhdpi" to "160px x 48px",
        "hdpi" to "121px x 36px",
        "mdpi" to "80px x 24px",
    ).map { (dpi, dim) ->
        "drawable-$dpi" to dim
    }.toMap()

    private val logoMusicResourceDirectoryNames = mapOf(
        "xxxhdpi" to "576px x 200px",
        "xxhdpi" to "432px x 150px",
        "xhdpi" to "288px x 100px",
        "hdpi" to "217px x 76px",
        "mdpi" to "144px x 50px",
    ).map { (dpi, dim) ->
        "drawable-$dpi" to dim
    }.toMap()

    private val ytmMusicLogoResourceDirectoryNames = mapOf(
        "xxxhdpi" to "412px x 144px",
        "xxhdpi" to "309px x 108px",
        "xhdpi" to "206px x 72px",
        "hdpi" to "155px x 54px",
        "mdpi" to "103px x 36px",
    ).map { (dpi, dim) ->
        "drawable-$dpi" to dim
    }.toMap()

    private val headerIconResourceFileNames = arrayOf(
        "action_bar_logo",
        "logo_music",
        "ytm_logo"
    ).map { "$it.png" }.toTypedArray()

    private val headerIconResourceGroups =
        actionBarLogoResourceDirectoryNames.keys.map { directory ->
            ResourceGroup(
                directory, *headerIconResourceFileNames
            )
        }

    private val getDescription = {
        var descriptionBody = """
            The header to apply to the app.
            
            Patch option '$DEFAULT_HEADER_KEY' applies only when:

            1. Patch 'Custom branding icon for YouTube Music' is included.
            2. Patch option for 'Custom branding icon for YouTube Music' is selected from the preset.
            
            If a path to a folder is provided, the folder must contain one or more of the following folders, depending on the DPI of the device:

            ${actionBarLogoResourceDirectoryNames.keys.joinToString("\n") { "- $it" }}

            Each of the folders must contain all of the following files:
            
            ${headerIconResourceFileNames.joinToString("\n") { "- $it" }}
            """

        mapOf(
            "action_bar_logo.png" to actionBarLogoResourceDirectoryNames,
            "logo_music.png" to logoMusicResourceDirectoryNames,
            "ytm_logo.png" to ytmMusicLogoResourceDirectoryNames
        ).forEach { (images, directoryNames) ->
            descriptionBody += """
            The image '$images' dimensions must be as follows:
            
            ${directoryNames.map { (dpi, dim) -> "- $dpi: $dim" }.joinToString("\n")}
            """
        }

        descriptionBody.trimIndentMultiline()
    }

    private val CustomHeader = stringPatchOption(
        key = "CustomHeader",
        default = DEFAULT_HEADER_VALUE,
        values = mapOf(
            DEFAULT_HEADER_KEY to DEFAULT_HEADER_VALUE
        ),
        title = "Custom header",
        description = getDescription(),
        required = true,
    )

    override fun execute(context: ResourceContext) {

        // Check patch options first.
        val customHeader = CustomHeader
            .underBarOrThrow()

        val customBrandingIconType = ResourceUtils.getIconType()
        val customBrandingIconIncluded = customBrandingIconType != "default"

        val warnings = "WARNING: Invalid header path: $customHeader. Does not apply patches."

        if (customHeader != DEFAULT_HEADER_VALUE) {
            context.copyFile(
                headerIconResourceGroups,
                customHeader,
                warnings
            )
        } else if (customBrandingIconIncluded) {
            headerIconResourceGroups.let { resourceGroups ->
                resourceGroups.forEach {
                    context.copyResources("music/branding/$customBrandingIconType/header", it)
                }
            }
        } else {
            println(warnings)
        }

    }
}
