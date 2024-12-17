package app.revanced.patches.music.layout.header

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.CUSTOM_HEADER_FOR_YOUTUBE_MUSIC
import app.revanced.patches.music.utils.settings.ResourceUtils.getIconType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.copyFile
import app.revanced.util.copyResources
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.underBarOrThrow
import app.revanced.util.valueOrThrow

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

private val changeHeaderBytecodePatch = bytecodePatch(
    description = "changeHeaderBytecodePatch"
) {
    execute {
        /**
         * New Header has been added from YouTube Music v7.04.51.
         *
         * The new header's file names are  'action_bar_logo_ringo2.png' and 'ytm_logo_ringo2.png'.
         * The only difference between the existing header and the new header is the dimensions of the image.
         *
         * The affected patch is [changeHeaderPatch].
         *
         * TODO: Add a new header image file to [changeHeaderPatch] later.
         */
        if (!headerSwitchConfigFingerprint.resolvable()) {
            return@execute
        }
        headerSwitchConfigFingerprint.injectLiteralInstructionBooleanCall(
            45617851L,
            "0x0"
        )
    }
}

@Suppress("unused")
val changeHeaderPatch = resourcePatch(
    CUSTOM_HEADER_FOR_YOUTUBE_MUSIC.title,
    CUSTOM_HEADER_FOR_YOUTUBE_MUSIC.summary,
    use = false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        changeHeaderBytecodePatch,
        settingsPatch,
    )

    val customHeaderOption = stringOption(
        key = "customHeader",
        default = DEFAULT_HEADER_VALUE,
        values = mapOf(
            DEFAULT_HEADER_KEY to DEFAULT_HEADER_VALUE
        ),
        title = "Custom header",
        description = getDescription(),
        required = true,
    )

    execute {
        // Check patch options first.
        var customHeader = customHeaderOption
            .underBarOrThrow()

        val isPath = customHeader != DEFAULT_HEADER_VALUE
        val customBrandingIconType = getIconType()
        val customBrandingIconIncluded = customBrandingIconType != "default"
        customHeader = customHeaderOption.valueOrThrow()

        val warnings = "WARNING: Invalid header path: $customHeader. Does not apply patches."

        if (isPath) {
            copyFile(
                headerIconResourceGroups,
                customHeader,
                warnings
            )
        } else if (customBrandingIconIncluded) {
            headerIconResourceGroups.let { resourceGroups ->
                resourceGroups.forEach {
                    copyResources("music/branding/$customBrandingIconType/header", it)
                }
            }
        } else {
            println(warnings)
        }

        updatePatchStatus(CUSTOM_HEADER_FOR_YOUTUBE_MUSIC)

    }
}

