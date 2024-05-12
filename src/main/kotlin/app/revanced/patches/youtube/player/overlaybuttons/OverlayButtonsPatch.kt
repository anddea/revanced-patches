package app.revanced.patches.youtube.player.overlaybuttons

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fix.fullscreen.FullscreenButtonViewStubPatch
import app.revanced.patches.youtube.utils.fix.suggestedvideoendscreen.SuggestedVideoEndScreenPatch
import app.revanced.patches.youtube.utils.integrations.Constants.OVERLAY_BUTTONS_PATH
import app.revanced.patches.youtube.utils.playercontrols.PlayerControlsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.doRecursively
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

@Suppress("DEPRECATION", "unused")
object OverlayButtonsPatch : BaseResourcePatch(
    name = "Overlay buttons",
    description = "Adds an option to display overlay buttons in the video player.",
    dependencies = setOf(
        FullscreenButtonViewStubPatch::class,
        PlayerControlsPatch::class,
        SettingsPatch::class,
        SuggestedVideoEndScreenPatch::class,
        OverlayButtonsBytecodePatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val DEFAULT_MARGIN = "0.0dip"
    private const val WIDER_MARGIN = "6.0dip"

    private const val DEFAULT_ICON_KEY = "Rounded"

    private val iconTypes = mapOf(
        "Bold" to "bold",
        DEFAULT_ICON_KEY to "rounded",
        "Thin" to "thin"
    )

    private val IconType by stringPatchOption(
        key = "IconType",
        default = DEFAULT_ICON_KEY,
        values = iconTypes,
        title = "Icon type",
        description = "Apply icon type"
    )

    private val BottomMargin by stringPatchOption(
        key = "BottomMargin",
        default = DEFAULT_MARGIN,
        values = mapOf(
            "Wider" to WIDER_MARGIN,
            "Default" to DEFAULT_MARGIN
        ),
        title = "Bottom margin",
        description = "Apply bottom margin to Overlay buttons and Timestamp"
    )

    override fun execute(context: ResourceContext) {

        /**
         * Inject hook
         */
        arrayOf(
            "AlwaysRepeat;",
            "CopyVideoUrl;",
            "CopyVideoUrlTimestamp;",
            "ExternalDownload;",
            "SpeedDialog;",
            "TimeOrderedPlaylist;"
        ).forEach { className ->
            PlayerControlsPatch.hookOverlayButtons("$OVERLAY_BUTTONS_PATH/$className")
        }

        /**
         * Copy resources
         */
        arrayOf(
            ResourceGroup(
                "drawable",
                "playlist_repeat_button.xml",
                "playlist_shuffle_button.xml",
                "revanced_repeat_icon.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/overlaybuttons/shared", resourceGroup)
        }

        IconType?.let { iconType ->
            val iconValue = iconType.lowercase()
            val commonResources = arrayOf(
                "ic_fullscreen_vertical_button.png",
                "ic_vr.png",
                "quantum_ic_fullscreen_exit_grey600_24.png",
                "quantum_ic_fullscreen_exit_white_24.png",
                "quantum_ic_fullscreen_grey600_24.png",
                "quantum_ic_fullscreen_white_24.png",
                "revanced_time_ordered_playlist.png",
                "revanced_copy_icon.png",
                "revanced_copy_icon_with_time.png",
                "revanced_download_icon.png",
                "revanced_speed_icon.png",
                "revanced_whitelist_icon.png",
                "yt_fill_arrow_repeat_white_24.png",
                "yt_outline_arrow_repeat_1_white_24.png",
                "yt_outline_arrow_shuffle_1_white_24.png",
                "yt_outline_screen_full_exit_white_24.png",
                "yt_outline_screen_full_white_24.png"
            )
            val specificResources = if (iconValue == "thin") {
                arrayOf("yt_outline_screen_vertical_vd_theme_24.xml")
            } else {
                arrayOf("yt_outline_screen_vertical_vd_theme_24.png")
            }
            val resources = commonResources + specificResources
            resources.forEach { resource ->
                val folderName = if (resource.endsWith(".xml")) "drawable" else "drawable-xxhdpi"
                context.copyResources("youtube/overlaybuttons/$iconValue", ResourceGroup(folderName, resource))
            }
        }

        /**
         * Merge xml nodes from the host to their real xml files
         */
        context.copyXmlNode(
            "youtube/overlaybuttons/shared/host",
            "layout/youtube_controls_bottom_ui_container.xml",
            "android.support.constraint.ConstraintLayout"
        )

        val marginBottom = "$BottomMargin"

        // For newer versions of YouTube (19.09.xx+), there's a new layout file for fullscreen button
        try {
            context.xmlEditor["res/layout/youtube_controls_fullscreen_button.xml"].use { editor ->
                editor.file.doRecursively loop@{ node ->
                    if (node !is Element) return@loop

                    if (node.getAttribute("android:id").endsWith("_button")) {
                        node.setAttribute("android:layout_marginBottom", marginBottom)
                        node.setAttribute("android:paddingLeft", "0.0dip")
                        node.setAttribute("android:paddingRight", "0.0dip")
                        node.setAttribute("android:paddingBottom", "22.0dip")
                        if (!node.getAttribute("android:layout_height").equals("0.0dip") &&
                            !node.getAttribute("android:layout_width").equals("0.0dip")
                        ) {
                            node.setAttribute("android:layout_height", "48.0dip")
                            node.setAttribute("android:layout_width", "48.0dip")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Do nothing
        }

        context.xmlEditor["res/layout/youtube_controls_bottom_ui_container.xml"].use { editor ->
            editor.file.doRecursively loop@{ node ->
                if (node !is Element) return@loop

                // Change the relationship between buttons
                node.getAttributeNode("yt:layout_constraintRight_toLeftOf")
                    ?.let { attribute ->
                        if (attribute.textContent == "@id/fullscreen_button") {
                            attribute.textContent = "@+id/speed_dialog_button"
                        }
                    }

                // Adjust TimeBar and Chapter bottom padding
                arrayOf(
                    "@id/time_bar_chapter_title" to "16.0dip",
                    "@id/timestamps_container" to "14.0dip"
                ).forEach { (id, replace) ->
                    node.getAttributeNode("android:id")?.let { attribute ->
                        if (attribute.textContent == id) {
                            node.getAttributeNode("android:paddingBottom").textContent =
                                replace
                        }
                    }
                }

                if (node.getAttribute("android:id") == "@id/youtube_controls_fullscreen_button_stub") {
                    node.setAttribute("android:layout_marginBottom", marginBottom)
                    if (!node.getAttribute("android:layout_height").equals("0.0dip") &&
                        !node.getAttribute("android:layout_width").equals("0.0dip")
                    ) {
                        node.setAttribute("android:layout_height", "48.0dip")
                        node.setAttribute("android:layout_width", "48.0dip")
                    }
                }

                if (node.getAttribute("android:id").endsWith("_button")) {
                    node.setAttribute("android:layout_marginBottom", marginBottom)
                    node.setAttribute("android:paddingLeft", "0.0dip")
                    node.setAttribute("android:paddingRight", "0.0dip")
                    node.setAttribute("android:paddingBottom", "22.0dip")
                    if (!node.getAttribute("android:layout_height").equals("0.0dip") &&
                        !node.getAttribute("android:layout_width").equals("0.0dip")
                    ) {
                        node.setAttribute("android:layout_height", "48.0dip")
                        node.setAttribute("android:layout_width", "48.0dip")
                    }
                } else if (node.getAttribute("android:id") == "@id/time_bar_chapter_title_container" ||
                    node.getAttribute("android:id") == "@id/timestamps_container"
                ) {
                    node.setAttribute("android:layout_marginBottom", marginBottom)
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: OVERLAY_BUTTONS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
