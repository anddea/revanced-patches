package app.revanced.patches.youtube.overlaybutton.general

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patches.youtube.overlaybutton.alwaysrepeat.AlwaysRepeatPatch
import app.revanced.patches.youtube.overlaybutton.download.hook.DownloadButtonHookPatch
import app.revanced.patches.youtube.overlaybutton.download.pip.DisablePiPPatch
import app.revanced.patches.youtube.utils.integrations.Constants.OVERLAY_BUTTONS_PATH
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
    description = "Add overlay buttons to the player.",
    dependencies = [
        AlwaysRepeatPatch::class,
        DisablePiPPatch::class,
        DownloadButtonHookPatch::class,
        OverrideSpeedHookPatch::class,
        PlayerButtonHookPatch::class,
        PlayerControlsPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoIdPatch::class
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
                "18.45.43"
            ]
        )
    ]
)
@Suppress("unused")
object OverlayButtonsPatch : ResourcePatch() {
    private val OutlineIcon by booleanPatchOption(
        key = "OutlineIcon",
        default = false,
        title = "Outline icons",
        description = "Apply the outline icon",
        required = true
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
            "SpeedDialog"
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

        if (OutlineIcon == true) {
            arrayOf(
                ResourceGroup(
                    "drawable-xxhdpi",
                    "ic_fullscreen_vertical_button.png",
                    "quantum_ic_fullscreen_exit_grey600_24.png",
                    "quantum_ic_fullscreen_exit_white_24.png",
                    "quantum_ic_fullscreen_grey600_24.png",
                    "quantum_ic_fullscreen_white_24.png",
                    "revanced_copy_icon.png",
                    "revanced_copy_icon_with_time.png",
                    "revanced_download_icon.png",
                    "revanced_speed_icon.png",
                    "yt_fill_arrow_repeat_white_24.png",
                    "yt_outline_arrow_repeat_1_white_24.png",
                    "yt_outline_arrow_shuffle_1_white_24.png",
                    "yt_outline_screen_full_exit_white_24.png",
                    "yt_outline_screen_full_white_24.png"
                )
            ).forEach { resourceGroup ->
                context.copyResources("youtube/overlaybuttons/outline", resourceGroup)
            }
        } else {
            arrayOf(
                ResourceGroup(
                    "drawable-xxhdpi",
                    "ic_fullscreen_vertical_button.png",
                    "ic_vr.png",
                    "quantum_ic_fullscreen_exit_grey600_24.png",
                    "quantum_ic_fullscreen_exit_white_24.png",
                    "quantum_ic_fullscreen_grey600_24.png",
                    "quantum_ic_fullscreen_white_24.png",
                    "revanced_copy_icon.png",
                    "revanced_copy_icon_with_time.png",
                    "revanced_download_icon.png",
                    "revanced_speed_icon.png",
                    "yt_fill_arrow_repeat_white_24.png",
                    "yt_outline_arrow_repeat_1_white_24.png",
                    "yt_outline_arrow_shuffle_1_white_24.png",
                    "yt_outline_screen_full_exit_white_24.png",
                    "yt_outline_screen_full_white_24.png",
                    "yt_outline_screen_vertical_vd_theme_24.png"
                )
            ).forEach { resourceGroup ->
                context.copyResources("youtube/overlaybuttons/default", resourceGroup)
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

        context.xmlEditor["res/layout/youtube_controls_bottom_ui_container.xml"].use { editor ->
            editor.file.doRecursively loop@{
                if (it !is Element) return@loop

                // Change the relationship between buttons
                it.getAttributeNode("yt:layout_constraintRight_toLeftOf")?.let { attribute ->
                    if (attribute.textContent == "@id/fullscreen_button") {
                        attribute.textContent = "@+id/speed_dialog_button"
                    }
                }

                // Adjust Fullscreen Button size and padding
                it.getAttributeNode("android:id")?.let { attribute ->
                    if (attribute.textContent == "@id/fullscreen_button") {
                        arrayOf(
                            "0.0dip" to arrayOf("paddingLeft", "paddingRight"),
                            "22.0dip" to arrayOf("paddingBottom"),
                            "48.0dip" to arrayOf("layout_height", "layout_width")
                        ).forEach { (replace, attributes) ->
                            attributes.forEach { name ->
                                it.getAttributeNode("android:$name").textContent = replace
                            }
                        }
                    }
                }

                // Adjust TimeBar and Chapter bottom padding
                arrayOf(
                    "@id/time_bar_chapter_title" to "14.0dip",
                    "@id/timestamps_container" to "12.0dip"
                ).forEach { (id, replace) ->
                    it.getAttributeNode("android:id")?.let { attribute ->
                        if (attribute.textContent == id) {
                            it.getAttributeNode("android:paddingBottom").textContent = replace
                        }
                    }
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
}