package app.revanced.patches.youtube.player.overlaybuttons

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.dismiss.dismissPlayerHookPatch
import app.revanced.patches.youtube.utils.extension.Constants.OVERLAY_BUTTONS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.fix.bottomui.cfBottomUIPatch
import app.revanced.patches.youtube.utils.patch.PatchList.OVERLAY_BUTTONS
import app.revanced.patches.youtube.utils.pip.pipStateHookPatch
import app.revanced.patches.youtube.utils.playercontrols.injectControl
import app.revanced.patches.youtube.utils.playercontrols.playerControlsPatch
import app.revanced.patches.youtube.utils.playlist.playlistPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.video.information.videoEndMethod
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.doRecursively
import app.revanced.util.lowerCaseOrThrow
import org.w3c.dom.Element

private const val EXTENSION_ALWAYS_REPEAT_CLASS_DESCRIPTOR =
    "$UTILS_PATH/AlwaysRepeatPatch;"

private val overlayButtonsBytecodePatch = bytecodePatch(
    description = "overlayButtonsBytecodePatch"
) {
    dependsOn(videoInformationPatch)

    execute {

        // region patch for always repeat

        videoEndMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_ALWAYS_REPEAT_CLASS_DESCRIPTOR->alwaysRepeat()Z
                    move-result v0
                    if-eqz v0, :end
                    return-void
                    """, ExternalLabel("end", getInstruction(0))
            )
        }

        // endregion

    }
}

private const val DEFAULT_ICON = "rounded"

@Suppress("unused")
val overlayButtonsPatch = resourcePatch(
    OVERLAY_BUTTONS.title,
    OVERLAY_BUTTONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        overlayButtonsBytecodePatch,
        cfBottomUIPatch,
        dismissPlayerHookPatch,
        pipStateHookPatch,
        playerControlsPatch,
        playlistPatch,
        sharedResourceIdPatch,
        settingsPatch,
    )

    val iconTypeOption = stringOption(
        key = "iconType",
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

    val changeTopButtons by booleanOption(
        key = "changeTopButtons",
        default = false,
        title = "Change top buttons",
        description = "Change the icons at the top of the player.",
        required = true
    )

    // TODO: Resize buttons and restore bottom margin options.

    execute {

        // Check patch options first.
        val iconType = iconTypeOption
            .lowerCaseOrThrow()

        // Inject hooks for overlay buttons.
        setOf(
            "AlwaysRepeatButton",
            "CopyVideoUrlButton",
            "CopyVideoUrlTimestampButton",
            "ExternalDownloadButton",
            "GeminiButton",
            "MuteVolumeButton",
            "PlayAllButton",
            "PlaybackSpeedDialogButton",
            "WhitelistButton",
        ).forEach { className ->
            injectControl("$OVERLAY_BUTTONS_PATH/${className};", false)
        }

        // Copy necessary resources for the overlay buttons.
        copyResources(
            "youtube/overlaybuttons/shared",
            ResourceGroup(
                "drawable",
                "playlist_repeat_button.xml",
                "playlist_shuffle_button.xml",
                "revanced_repeat_button.xml",
                "revanced_mute_volume_button.xml",
            )
        )

        // Apply the selected icon type to the overlay buttons.
        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).forEach { dpi ->
            copyResources(
                "youtube/overlaybuttons/$iconType",
                ResourceGroup(
                    "drawable-$dpi",
                    "ic_vr.png",
                    "quantum_ic_fullscreen_exit_grey600_24.png",
                    "quantum_ic_fullscreen_exit_white_24.png",
                    "quantum_ic_fullscreen_grey600_24.png",
                    "quantum_ic_fullscreen_white_24.png",
                    "revanced_copy_button.png",
                    "revanced_copy_timestamp_button.png",
                    "revanced_external_download_button.png",
                    "revanced_gemini_button.png",
                    "revanced_play_all_button.png",
                    "revanced_playback_speed_dialog_button.png",
                    "revanced_volume_muted_button.png",
                    "revanced_volume_unmuted_button.png",
                    "revanced_whitelist_button.png",
                    "yt_fill_arrow_repeat_white_24.png",
                    "yt_outline_arrow_repeat_1_white_24.png",
                    "yt_outline_arrow_shuffle_1_white_24.png",
                    "yt_outline_arrow_shuffle_black_24.png",
                    "yt_outline_list_play_arrow_black_24.png",
                    "yt_outline_list_play_arrow_white_24.png",
                    "yt_outline_screen_full_exit_white_24.png",
                    "yt_outline_screen_full_vd_theme_24.png",
                    "yt_outline_screen_full_white_24.png",
                    "yt_outline_screen_vertical_vd_theme_24.png",
                ),
                ResourceGroup(
                    "drawable",
                    "yt_outline_screen_vertical_vd_theme_24.xml"
                )
            )
        }

        // Merge XML nodes from the host to their respective XML files.
        copyXmlNode(
            "youtube/overlaybuttons/shared/host",
            "layout/youtube_controls_bottom_ui_container.xml",
            "android.support.constraint.ConstraintLayout"
        )

        document("res/layout/youtube_controls_bottom_ui_container.xml").use { document ->
            document.doRecursively loop@{ node ->
                if (node !is Element) return@loop

                // Change the relationship between buttons
                node.getAttributeNode("yt:layout_constraintRight_toLeftOf")
                    ?.let { attribute ->
                        if (attribute.textContent == "@id/fullscreen_button") {
                            attribute.textContent = "@+id/revanced_gemini_button"
                        }
                    }
            }
        }

        if (changeTopButtons == true) {
            // Apply the selected icon type to the top buttons.
            arrayOf(
                "xxxhdpi",
                "xxhdpi",
                "xhdpi",
                "hdpi",
                "mdpi"
            ).forEach { dpi ->
                copyResources(
                    "youtube/overlaybuttons/$iconType",
                    ResourceGroup(
                        "drawable-$dpi",
                        "yt_outline_gear_white_24.png",
                        "yt_outline_chevron_down_white_24.png",
                        "quantum_ic_closed_caption_off_grey600_24.png",
                        "quantum_ic_closed_caption_off_white_24.png",
                        "quantum_ic_closed_caption_white_24.png"
                    )
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: OVERLAY_BUTTONS"
            ),
            OVERLAY_BUTTONS
        )

        // endregion

    }
}
