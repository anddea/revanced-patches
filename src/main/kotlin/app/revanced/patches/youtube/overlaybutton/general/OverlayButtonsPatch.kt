package app.revanced.patches.youtube.overlaybutton.general

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.overlaybutton.alwaysrepeat.AlwaysRepeatPatch
import app.revanced.patches.youtube.overlaybutton.download.hook.DownloadButtonHookPatch
import app.revanced.patches.youtube.overlaybutton.download.pip.DisablePiPPatch
import app.revanced.patches.youtube.overlaybutton.whitelist.WhitelistPatch
import app.revanced.patches.youtube.utils.integrations.Constants.OVERLAY_BUTTONS_PATH
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.youtube.utils.overridespeed.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.playerbutton.PlayerButtonHookPatch
import app.revanced.patches.youtube.utils.playercontrols.PlayerControlsPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.videoid.general.VideoIdPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.doRecursively
import org.w3c.dom.Element

@Patch(
    name = "Overlay buttons",
    description = "Adds an option to display overlay buttons in the video player.",
    dependencies = [
        AlwaysRepeatPatch::class,
        DisablePiPPatch::class,
        DownloadButtonHookPatch::class,
        MainActivityResolvePatch::class,
        OverrideSpeedHookPatch::class,
        PlayerButtonHookPatch::class,
        PlayerControlsPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoIdPatch::class,
        WhitelistPatch::class
    ],
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
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43",
                "19.15.36",
                "19.16.38"
            ]
        )
    ]
)
@Suppress("unused")
object OverlayButtonsPatch : ResourcePatch() {
    private const val DEFAULT_MARGIN = "0.0dip"
    private const val WIDER_MARGIN = "6.0dip"
    private const val LAYOUT_HEIGHT = "android:layout_height"
    private const val LAYOUT_WIDTH = "android:layout_width"
    private const val ANDROID_ID = "android:id"

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
            "AlwaysRepeat",
            "CopyVideoUrl",
            "CopyVideoUrlTimestamp",
            "ExternalDownload",
            "SpeedDialog",
            "Whitelists",
            "PlaylistFromChannelVideos"
        ).forEach { patch ->
            PlayerControlsPatch.initializeControl("$OVERLAY_BUTTONS_PATH/$patch;")
            PlayerControlsPatch.injectVisibility("$OVERLAY_BUTTONS_PATH/$patch;")
        }

        /**
         * Copy arrays
         */
        context.copyXmlNode("youtube/overlaybuttons/shared/host", "values/arrays.xml", "resources")

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
            val specificResources = if (iconValue == "thin")
                arrayOf("yt_outline_screen_vertical_vd_theme_24.xml")
            else
                arrayOf("yt_outline_screen_vertical_vd_theme_24.png")

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
            context.document["res/layout/youtube_controls_fullscreen_button.xml"].use { editor ->
                editor.doRecursively loop@{ node ->
                    if (node !is Element) return@loop

                    if (!node.getAttribute(ANDROID_ID).endsWith("_button")) return@loop

                    adjustNodeMargins(node, marginBottom)
                }
            }
        } catch (_: Exception) { /* Do nothing */ }

        context.document["res/layout/youtube_controls_bottom_ui_container.xml"].use { editor ->

            editor.doRecursively loop@{ node ->

                if (node !is Element) return@loop

                // Change the relationship between buttons
                node.getAttributeNode("yt:layout_constraintRight_toLeftOf")?.let { attribute ->
                        if (attribute.textContent == "@id/fullscreen_button")
                            attribute.textContent = "@+id/speed_dialog_button"
                }

                // Adjust TimeBar and Chapter bottom padding
                arrayOf(
                    "@id/time_bar_chapter_title" to "16.0dip",
                    "@id/timestamps_container" to "14.0dip"
                ).forEach { (id, replace) ->
                    node.getAttributeNode("android:id")?.let { attribute ->
                        if (attribute.textContent == id)
                            node.getAttributeNode("android:paddingBottom").textContent = replace
                    }
                }

                val nodeId = node.getAttribute(ANDROID_ID)

                if (nodeId == "@id/youtube_controls_fullscreen_button_stub") {
                    node.setAttribute("android:layout_marginBottom", marginBottom)


                    if (node.getAttribute(LAYOUT_HEIGHT) != DEFAULT_MARGIN &&
                        node.getAttribute(LAYOUT_WIDTH) != DEFAULT_MARGIN) {

                        node.setAttribute(LAYOUT_HEIGHT, "48.0dip")
                        node.setAttribute(LAYOUT_WIDTH, "48.0dip")
                    }
                }

                when {
                    nodeId == "@id/time_bar_chapter_title_container" || nodeId == "@id/timestamps_container" ->
                        node.setAttribute("android:layout_marginBottom", marginBottom)

                    nodeId.endsWith("_button") ->
                        adjustNodeMargins(node, marginBottom)
                }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: OVERLAY_BUTTONS",
                "SETTINGS: OVERLAY_BUTTONS"
            )
        )

        SettingsPatch.updatePatchStatus("Overlay buttons")

    }

    /**
     * Adjust the margins of the node with the custom margin bottom, and sets the padding for the node
     */
    private fun adjustNodeMargins(node: Element, marginBottom: String) {
        node.setAttribute("android:layout_marginBottom", marginBottom)
        node.setAttribute("android:paddingLeft", DEFAULT_MARGIN)
        node.setAttribute("android:paddingRight", DEFAULT_MARGIN)
        node.setAttribute("android:paddingBottom", "22.0dip")

        if (node.getAttribute(LAYOUT_HEIGHT) != DEFAULT_MARGIN &&
            node.getAttribute(LAYOUT_WIDTH) != DEFAULT_MARGIN) {

            node.setAttribute(LAYOUT_HEIGHT, "48.0dip")
            node.setAttribute(LAYOUT_WIDTH, "48.0dip")
        }
    }
}