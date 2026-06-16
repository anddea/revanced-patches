package app.morphe.patches.youtube.layout.actionbuttons

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
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
    compatibleWith(COMPATIBILITY_YOUTUBE)

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
        fun existingDrawableNames(): MutableSet<String> {
            val names = mutableSetOf<String>()

            document("res/values/drawables.xml").use { document ->
                val drawableNodes = document.getElementsByTagName("drawable")
                for (i in 0 until drawableNodes.length) {
                    val node = drawableNodes.item(i)
                    if (node.attributes?.getNamedItem("name") != null) {
                        names.add(node.attributes.getNamedItem("name").nodeValue)
                    }
                }
            }

            get("res").listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("drawable") }
                ?.forEach { directory ->
                    directory.listFiles()
                        ?.filter { it.isFile }
                        ?.forEach { file ->
                            names.add(file.name.substringBeforeLast(".").removeSuffix(".9"))
                        }
                }

            return names
        }

        fun addDrawableAliases(aliases: Map<String, String>): Pair<Int, Int> {
            var addedAliases = 0
            var skippedAliases = 0
            val existingDrawableNames = existingDrawableNames()

            document("res/values/drawables.xml").use { document ->
                val rootNode = document.documentElement

                aliases.forEach { (name, value) ->
                    val targetName = value.removePrefix("@drawable/")
                    if (name in existingDrawableNames || targetName !in existingDrawableNames) {
                        skippedAliases++
                        return@forEach
                    }

                    val element = document.createElement("drawable")
                    element.setAttribute("name", name)
                    element.textContent = value
                    rootNode.appendChild(element)
                    existingDrawableNames.add(name)
                    addedAliases++
                }
            }

            return addedAliases to skippedAliases
        }

        val (addedAliases, skippedAliases) = addDrawableAliases(
            mapOf(
                // Modern Shorts action button names requested by server-side layouts.
                // YouTube 19.16.39 already has the original white shadowed buttons, so
                // keep the legacy look and only add missing aliases.
                "youtube_shorts_like_outline_32dp" to "@drawable/ic_right_like_off_shadowed",
                "youtube_shorts_like_fill_32dp" to "@drawable/ic_right_like_on_shadowed",
                "youtube_shorts_thumbs_up_outline_28dp" to "@drawable/ic_right_like_off_shadowed",
                "youtube_shorts_thumbs_up_fill_28dp" to "@drawable/ic_right_like_on_shadowed",
                "youtube_shorts_dislike_outline_32dp" to "@drawable/ic_right_dislike_off_shadowed",
                "youtube_shorts_dislike_fill_32dp" to "@drawable/ic_right_dislike_on_shadowed",
                "youtube_shorts_thumbs_down_outline_28dp" to "@drawable/ic_right_dislike_off_shadowed",
                "youtube_shorts_thumbs_down_fill_28dp" to "@drawable/ic_right_dislike_on_shadowed",
                "youtube_shorts_comment_outline_28dp" to "@drawable/ic_right_comment_shadowed",
                "youtube_shorts_comment_outline_32dp" to "@drawable/ic_right_comment_shadowed",
                "youtube_shorts_share_outline_28dp" to "@drawable/ic_right_share_shadowed",
                "youtube_shorts_share_outline_32dp" to "@drawable/ic_right_share_shadowed",
                "youtube_shorts_remix_outline_28dp" to "@drawable/ic_remix_filled_white_shadowed",
                "youtube_shorts_remix_outline_32dp" to "@drawable/ic_remix_filled_white_shadowed",
                "youtube_shorts_heart_outline_28dp" to "@drawable/youtube_shorts_heart_off_32dp",
                "youtube_shorts_heart_outline_32dp" to "@drawable/youtube_shorts_heart_off_32dp",
                "youtube_shorts_heart_fill_28dp" to "@drawable/youtube_shorts_heart_on_32dp",
                "youtube_shorts_heart_fill_32dp" to "@drawable/youtube_shorts_heart_on_32dp",
                "youtube_shorts_save_outline_28dp" to "@drawable/yt_outline_bookmark_black_24",
                "youtube_shorts_save_outline_32dp" to "@drawable/yt_outline_bookmark_black_24",
                "youtube_shorts_save_fill_28dp" to "@drawable/yt_fill_bookmark_black_24",
                "youtube_shorts_save_fill_32dp" to "@drawable/yt_fill_bookmark_black_24",
                "youtube_shorts_save_fill_selected_32dp" to "@drawable/yt_fill_bookmark_black_24",
                "youtube_shorts_save_fill_unselected_32dp" to "@drawable/yt_outline_bookmark_black_24",
                "youtube_shorts_original_sound_16dp" to "@drawable/quantum_ic_music_note_white_24",
                "youtube_shorts_pivot_fab" to "@drawable/ic_youtube_shorts_24",

                // Other small Shorts resources introduced after 19.16.39.
                "ic_youtube_shorts_24_cairo" to "@drawable/ic_youtube_shorts_24",
                "shorts_creation_secondary_rounded_button_background" to "@drawable/shorts_creation_rounded_button_background",
            )
        )
        printInfo(
            "Custom Shorts action buttons: added $addedAliases drawable aliases, " +
                    "skipped $skippedAliases existing aliases."
        )

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
