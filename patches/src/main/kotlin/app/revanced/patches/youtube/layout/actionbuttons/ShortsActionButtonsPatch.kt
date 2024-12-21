package app.revanced.patches.youtube.layout.actionbuttons

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.CUSTOM_SHORTS_ACTION_BUTTONS
import app.revanced.patches.youtube.utils.playservice.is_19_36_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.inputStreamFromBundledResourceOrThrow
import app.revanced.util.lowerCaseOrThrow
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private const val DEFAULT_ICON = "cairo"
private const val YOUTUBE_ICON = "youtube"

private val sizeArray = arrayOf(
    "xxxhdpi",
    "xxhdpi",
    "xhdpi",
    "hdpi",
    "mdpi"
)

private val drawableDirectories = sizeArray.map { "drawable-$it" }

@Suppress("unused")
val shortsActionButtonsPatch = resourcePatch(
    CUSTOM_SHORTS_ACTION_BUTTONS.title,
    CUSTOM_SHORTS_ACTION_BUTTONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch
    )

    val iconTypeOption = stringOption(
        key = "iconType",
        default = DEFAULT_ICON,
        values = mapOf(
            "Cairo" to DEFAULT_ICON,
            "Outline" to "outline",
            "OutlineCircle" to "outlinecircle",
            "Round" to "round",
            "YoutubeOutline" to "youtubeoutline",
            "YouTube" to YOUTUBE_ICON
        ),
        title = "Shorts icon style ",
        description = "The style of the icons for the action buttons in the Shorts player.",
        required = true,
    )

    execute {

        // Check patch options first.
        val iconType = iconTypeOption
            .lowerCaseOrThrow()

        if (iconType == YOUTUBE_ICON) {
            println("INFO: Shorts action buttons will remain unchanged as it matches the original.")
            addPreference(CUSTOM_SHORTS_ACTION_BUTTONS)
            return@execute
        }

        val sourceResourceDirectory = "youtube/shorts/actionbuttons/$iconType"

        val resourceMap = ShortsActionButtons.entries.map { it.newResource to it.resources }
        val res = get("res")

        for ((toFileName, fromResourceArray) in resourceMap) {
            fromResourceArray.forEach { fromFileName ->
                drawableDirectories.forEach { drawableDirectory ->
                    val fromFile = "$drawableDirectory/$fromFileName.webp"
                    val fromPath = res.resolve(fromFile).toPath()
                    val toFile = "$drawableDirectory/$toFileName.webp"
                    val toPath = res.resolve(toFile).toPath()
                    val inputStreamForLegacy =
                        inputStreamFromBundledResourceOrThrow(sourceResourceDirectory, fromFile)
                    val inputStreamForNew =
                        inputStreamFromBundledResourceOrThrow(sourceResourceDirectory, fromFile)

                    Files.copy(inputStreamForLegacy, fromPath, StandardCopyOption.REPLACE_EXISTING)

                    if (is_19_36_or_greater) {
                        Files.copy(inputStreamForNew, toPath, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }

        copyResources(
            sourceResourceDirectory,
            ResourceGroup(
                "drawable",
                "ic_right_comment_32c.xml",
                "ic_right_dislike_off_32c.xml",
                "ic_right_like_off_32c.xml",
                "ic_right_share_32c.xml"
            )
        )

        addPreference(CUSTOM_SHORTS_ACTION_BUTTONS)

        if (iconType == DEFAULT_ICON) {
            return@execute
        }

        copyResources(
            "youtube/shorts/actionbuttons/shared",
            ResourceGroup(
                "drawable",
                "reel_camera_bold_24dp.xml",
                "reel_more_vertical_bold_24dp.xml",
                "reel_search_bold_24dp.xml"
            )
        )
    }
}

internal enum class ShortsActionButtons(val newResource: String, vararg val resources: String) {
    LIKE(
        "youtube_shorts_like_outline_32dp",
        // This replaces the new icon.
        "ic_right_like_off_shadowed",
    ),
    LIKE_FILLED(
        "youtube_shorts_like_fill_32dp",
        "ic_right_like_on_32c",
        // This replaces the new icon.
        "ic_right_like_on_shadowed",
    ),
    DISLIKE(
        "youtube_shorts_dislike_outline_32dp",
        // This replaces the new icon.
        "ic_right_dislike_off_shadowed",
    ),
    DISLIKE_FILLED(
        "youtube_shorts_dislike_fill_32dp",
        "ic_right_dislike_on_32c",
        // This replaces the new icon.
        "ic_right_dislike_on_shadowed",
    ),
    COMMENT(
        "youtube_shorts_comment_outline_32dp",
        // This replaces the new icon.
        "ic_right_comment_shadowed",
    ),
    SHARE(
        "youtube_shorts_share_outline_32dp",
        // This replaces the new icon.
        "ic_right_share_shadowed",
    ),
    REMIX(
        "youtube_shorts_remix_outline_32dp",
        "ic_remix_filled_white_24",
        // This replaces the new icon.
        "ic_remix_filled_white_shadowed",
    ),
}
