package app.morphe.patches.youtube.layout.actionbuttons

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.CUSTOM_SHORTS_ACTION_BUTTONS
import app.morphe.patches.youtube.utils.playservice.is_19_36_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.Utils.printInfo
import app.morphe.util.copyResources
import app.morphe.util.inputStreamFromBundledResource
import app.morphe.util.lowerCaseOrThrow
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
            printInfo("Shorts action buttons will remain unchanged as it matches the original.")
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
                    val fromFileResolved = res.resolve(fromFile)
                    val toFile = "$drawableDirectory/$toFileName.webp"
                    val toFileResolved = res.resolve(toFile)
                    val inputStreamForLegacy =
                        inputStreamFromBundledResource(sourceResourceDirectory, fromFile)

                    // Some directory is missing in the bundles.
                    if (inputStreamForLegacy != null && fromFileResolved.exists()) {
                        Files.copy(
                            inputStreamForLegacy,
                            fromFileResolved.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }

                    if (is_19_36_or_greater) {
                        val inputStreamForNew =
                            inputStreamFromBundledResource(sourceResourceDirectory, fromFile)

                        // Some directory is missing in the bundles.
                        if (inputStreamForNew != null && toFileResolved.exists()) {
                            Files.copy(
                                inputStreamForNew,
                                toFileResolved.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }
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
