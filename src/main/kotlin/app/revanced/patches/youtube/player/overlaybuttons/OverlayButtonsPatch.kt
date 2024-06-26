package app.revanced.patches.youtube.player.overlaybuttons

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fix.fullscreen.FullscreenButtonViewStubPatch
import app.revanced.patches.youtube.utils.integrations.Constants.OVERLAY_BUTTONS_PATH
import app.revanced.patches.youtube.utils.playercontrols.PlayerControlsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.doRecursively
import app.revanced.util.lowerCaseOrThrow
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

/**
 * Patch to add overlay buttons in the YouTube video player.
 *
 * This patch integrates various buttons such as copy URL, speed, repeat, etc., into the video player's
 * control overlay, providing enhanced functionality directly in the player interface.
 */
@Suppress("DEPRECATION", "unused")
object OverlayButtonsPatch : BaseResourcePatch(
    name = "Overlay buttons",
    description = "Adds options to display overlay buttons in the video player.",
    dependencies = setOf(
        FullscreenButtonViewStubPatch::class,
        PlayerControlsPatch::class,
        SettingsPatch::class,
        OverlayButtonsBytecodePatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val DEFAULT_MARGIN = "0.0dip"
    private const val WIDER_MARGIN = "6.0dip"

    private const val DEFAULT_ICON = "rounded"

    // Option to select icon type
    private val IconType = stringPatchOption(
        key = "IconType",
        default = DEFAULT_ICON,
        values = mapOf(
            "Bold" to "bold",
            "Rounded" to DEFAULT_ICON,
            "Thin" to "thin"
        ),
        title = "Icon type",
        description = "The icon type.",
        required = true
    )

    // Option to set bottom margin
    private val BottomMargin = stringPatchOption(
        key = "BottomMargin",
        default = DEFAULT_MARGIN,
        values = mapOf(
            "Wider" to WIDER_MARGIN,
            "Default" to DEFAULT_MARGIN
        ),
        title = "Bottom margin",
        description = "The bottom margin for the overlay buttons and timestamp.",
        required = true
    )

    /**
     * Main execution method for applying the patch.
     *
     * @param context The resource context for patching.
     */
    override fun execute(context: ResourceContext) {

        // Check patch options first.
        val iconType = IconType
            .lowerCaseOrThrow()

        val marginBottom = BottomMargin
            .lowerCaseOrThrow()

        // Inject hooks for overlay buttons.
        arrayOf(
            "AlwaysRepeat;",
            "CopyVideoUrl;",
            "CopyVideoUrlTimestamp;",
            "ExternalDownload;",
            "SpeedDialog;",
            "TimeOrderedPlaylist;",
            "Whitelists;"
        ).forEach { className ->
            PlayerControlsPatch.hookOverlayButtons("$OVERLAY_BUTTONS_PATH/$className")
        }

        // Copy necessary resources for the overlay buttons.
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

        // Apply the selected icon type to the overlay buttons.
        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).forEach { dpi ->
            context.copyResources(
                "youtube/overlaybuttons/$iconType",
                ResourceGroup(
                    "drawable-$dpi",
                    "ic_fullscreen_vertical_button.png",
                    "ic_vr.png",
                    "quantum_ic_fullscreen_exit_grey600_24.png",
                    "quantum_ic_fullscreen_exit_white_24.png",
                    "quantum_ic_fullscreen_grey600_24.png",
                    "quantum_ic_fullscreen_white_24.png",
                    "revanced_time_ordered_playlist_icon.png",
                    "revanced_copy_icon.png",
                    "revanced_copy_icon_with_time.png",
                    "revanced_download_icon.png",
                    "revanced_speed_icon.png",
                    "revanced_whitelist_icon.png",
                    "yt_fill_arrow_repeat_white_24.png",
                    "yt_outline_arrow_repeat_1_white_24.png",
                    "yt_outline_arrow_shuffle_1_white_24.png",
                    "yt_outline_screen_full_exit_white_24.png",
                    "yt_outline_screen_full_white_24.png",
                    "yt_outline_screen_vertical_vd_theme_24.png"
                ),
                ResourceGroup(
                    "drawable",
                    "yt_outline_screen_vertical_vd_theme_24.xml"
                )
            )
        }

        // Merge XML nodes from the host to their respective XML files.
        context.copyXmlNode(
            "youtube/overlaybuttons/shared/host",
            "layout/youtube_controls_bottom_ui_container.xml",
            "android.support.constraint.ConstraintLayout"
        )

        // Modify the layout of fullscreen button for newer YouTube versions (19.09.xx+)
        arrayOf(
            "youtube_controls_cf_fullscreen_button.xml",
            "youtube_controls_fullscreen_button.xml"
        ).forEach { xmlFile ->
            val targetXml = context["res"].resolve("layout").resolve(xmlFile)
            if (targetXml.exists()) {
                context.xmlEditor["res/layout/$xmlFile"].use { editor ->
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
            }
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

                // Adjust layout for fullscreen button stub
                if (node.getAttribute("android:id") == "@id/youtube_controls_fullscreen_button_stub") {
                    node.setAttribute("android:layout_marginBottom", marginBottom)
                    if (!node.getAttribute("android:layout_height").equals("0.0dip") &&
                        !node.getAttribute("android:layout_width").equals("0.0dip")
                    ) {
                        node.setAttribute("android:layout_height", "48.0dip")
                        node.setAttribute("android:layout_width", "48.0dip")
                    }
                }

                // Adjust margin and padding for other buttons
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
         * Add settings for the overlay buttons.
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: OVERLAY_BUTTONS"
            )
        )

        // Update the patch status in settings to reflect the applied changes
        SettingsPatch.updatePatchStatus(this)
    }
}
