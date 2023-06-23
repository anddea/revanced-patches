package app.revanced.patches.youtube.overlaybutton.general.patch

import app.revanced.extensions.doRecursively
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.overlaybutton.autorepeat.patch.AutoRepeatPatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.overridespeed.patch.OverrideSpeedHookPatch
import app.revanced.patches.youtube.utils.playercontrols.patch.PlayerControlsPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.utils.videoid.mainstream.patch.MainstreamVideoIdPatch
import app.revanced.util.integrations.Constants.BUTTON_PATH
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources
import app.revanced.util.resources.ResourceUtils.copyXmlNode
import org.w3c.dom.Element

@Patch
@Name("overlay-buttons")
@Description("Add overlay buttons to the player such as copy video link, auto-repeat, download and speed control.")
@DependsOn(
    [
        AutoRepeatPatch::class,
        MainstreamVideoIdPatch::class,
        OverrideSpeedHookPatch::class,
        PlayerControlsPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class OverlayButtonsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /**
         * Inject hook
         */
        arrayOf(
            "Download",
            "AutoRepeat",
            "CopyWithTimeStamp",
            "Copy",
            "Speed"
        ).forEach {
            PlayerControlsPatch.initializeControl("$BUTTON_PATH/$it;")
            PlayerControlsPatch.injectVisibility("$BUTTON_PATH/$it;")
        }

        /**
         * Copy arrays
         */
        context.copyXmlNode("youtube/overlaybuttons/host", "values/arrays.xml", "resources")


        /**
         * Copy resources
         */
        arrayOf(
            ResourceUtils.ResourceGroup(
                "drawable",
                "playlist_repeat_button.xml",
                "playlist_shuffle_button.xml",
                "revanced_repeat_icon.xml"
            ),
            ResourceUtils.ResourceGroup(
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
                "yt_outline_screen_full_white_24.png"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/overlaybuttons", resourceGroup)
        }

        /**
         * Merge xml nodes from the host to their real xml files
         */
        context.copyXmlNode(
            "youtube/overlaybuttons/host",
            "layout/youtube_controls_bottom_ui_container.xml",
            "android.support.constraint.ConstraintLayout"
        )

        context.xmlEditor["res/layout/youtube_controls_bottom_ui_container.xml"].use { editor ->
            editor.file.doRecursively loop@{
                if (it !is Element) return@loop

                // Change the relationship between buttons
                it.getAttributeNode("yt:layout_constraintRight_toLeftOf")?.let { attribute ->
                    if (attribute.textContent == "@id/fullscreen_button") {
                        attribute.textContent = "@+id/speed_button"
                    }
                }

                // Adjust Fullscreen Button size and padding
                val padding = "0.0dip" to arrayOf(
                    "paddingLeft",
                    "paddingRight",
                    "paddingTop",
                    "paddingBottom"
                )
                val size = "45.0dip" to arrayOf("layout_width", "layout_height")
                it.getAttributeNode("android:id")?.let { attribute ->
                    if (attribute.textContent == "@id/fullscreen_button") {
                        arrayOf(padding, size).forEach { (replace, attributes) ->
                            attributes.forEach { name ->
                                it.getAttributeNode("android:$name").textContent = replace
                            }
                        }
                    }
                }

                // Adjust TimeBar and Chapter bottom padding
                val timeBarChapter = "@id/time_bar_chapter_title" to "14.0dip"
                val timeStampContainer = "@id/timestamps_container" to "12.0dip"
                arrayOf(timeBarChapter, timeStampContainer).forEach { (id, replace) ->
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

        SettingsPatch.updatePatchStatus("overlay-buttons")

        return PatchResultSuccess()
    }
}