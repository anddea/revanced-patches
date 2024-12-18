package app.revanced.patches.youtube.layout.actionbuttons

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.CUSTOM_SHORTS_ACTION_BUTTONS
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.lowerCaseOrThrow

private const val DEFAULT_ICON = "cairo"
private const val YOUTUBE_ICON = "youtube"

@Suppress("unused")
val shortsActionButtonsPatch = resourcePatch(
    CUSTOM_SHORTS_ACTION_BUTTONS.title,
    CUSTOM_SHORTS_ACTION_BUTTONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val iconType = stringOption(
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
        val iconType = iconType
            .lowerCaseOrThrow()

        if (iconType == YOUTUBE_ICON) {
            println("INFO: Shorts action buttons will remain unchanged as it matches the original.")
            addPreference(CUSTOM_SHORTS_ACTION_BUTTONS)
            return@execute
        }

        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).forEach { dpi ->
            copyResources(
                "youtube/shorts/actionbuttons/$iconType",
                ResourceGroup(
                    "drawable-$dpi",
                    "ic_remix_filled_white_shadowed.webp",
                    "ic_right_comment_shadowed.webp",
                    "ic_right_dislike_off_shadowed.webp",
                    "ic_right_dislike_on_shadowed.webp",
                    "ic_right_like_off_shadowed.webp",
                    "ic_right_like_on_shadowed.webp",
                    "ic_right_share_shadowed.webp",

                    // for older versions only
                    "ic_remix_filled_white_24.webp",
                    "ic_right_dislike_on_32c.webp",
                    "ic_right_like_on_32c.webp"
                ),
                ResourceGroup(
                    "drawable",
                    "ic_right_comment_32c.xml",
                    "ic_right_dislike_off_32c.xml",
                    "ic_right_like_off_32c.xml",
                    "ic_right_share_32c.xml"
                )
            )
        }

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
