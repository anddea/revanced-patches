/*
 * Copyright (C) 2022-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Francesco Marastoni (https://github.com/Francesco146)
 * - Hoàng Gia Bảo (https://github.com/YT-Advanced)
 * - inotia00 (https://github.com/inotia00)
 * - MondayNitro (https://github.com/MondayNitro)
 * - rufusin (https://github.com/rufusin)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.youtube.player.overlaybuttons

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.dismiss.dismissPlayerHookPatch
import app.morphe.patches.youtube.utils.dismiss.hookDismissObserver
import app.morphe.patches.youtube.utils.extension.Constants.OVERLAY_BUTTONS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.fix.bottomui.cfBottomUIPatch
import app.morphe.patches.youtube.utils.patch.PatchList.OVERLAY_BUTTONS
import app.morphe.patches.youtube.utils.pip.pipStateHookPatch
import app.morphe.patches.youtube.utils.playercontrols.injectControl
import app.morphe.patches.youtube.utils.playercontrols.playerControlsPatch
import app.morphe.patches.youtube.utils.playlist.playlistPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.general.startpage.IntentActionFingerprint
import app.morphe.patches.youtube.general.startpage.IntentResolverFingerprint
import app.morphe.patches.youtube.video.information.videoEndMethod
import app.morphe.patches.youtube.video.information.hookVideoInformation
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.information.videoTimeHook
import app.morphe.util.ResourceGroup
import app.morphe.util.Utils.printWarn
import app.morphe.util.copyResources
import app.morphe.util.copyXmlNode
import app.morphe.util.doRecursively
import app.morphe.util.findFreeRegister
import app.morphe.util.lowerCaseOrThrow
import org.w3c.dom.Element

private const val EXTENSION_ALWAYS_REPEAT_CLASS_DESCRIPTOR =
    "$UTILS_PATH/AlwaysRepeatPatch;"
private const val EXTENSION_LOOP_SEGMENT_CLASS_DESCRIPTOR =
    "$OVERLAY_BUTTONS_PATH/LoopSegmentButton;"

private val overlayButtonsBytecodePatch = bytecodePatch(
    description = "overlayButtonsBytecodePatch"
) {
    dependsOn(
        dismissPlayerHookPatch,
        videoInformationPatch,
    )

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

        // region patch for loop segment button

        hookVideoInformation("$EXTENSION_LOOP_SEGMENT_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        hookDismissObserver("$EXTENSION_LOOP_SEGMENT_CLASS_DESCRIPTOR->onPlayerDismissed(I)V")
        videoTimeHook(EXTENSION_LOOP_SEGMENT_CLASS_DESCRIPTOR, "videoTimeChanged")

        videoEndMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_LOOP_SEGMENT_CLASS_DESCRIPTOR->videoEnded()Z
                    move-result v0
                    if-eqz v0, :end
                    return-void
                    """, ExternalLabel("end", getInstruction(0))
            )
        }

        IntentActionFingerprint.method.addHandleIntentHook()
        IntentResolverFingerprint.method.addHandleIntentHook()

        // endregion

    }
}

private fun MutableMethod.addHandleIntentHook() {
    val freeRegister = findFreeRegister(0)
    addInstructions(
        0, """
            move-object/from16 v$freeRegister, p1
            invoke-static { v$freeRegister }, $EXTENSION_LOOP_SEGMENT_CLASS_DESCRIPTOR->handleIntent(Landroid/content/Intent;)V
            """
    )
}

private const val MARGIN_MINIMUM = "0.1dip"
private const val MARGIN_DEFAULT = "2.5dip"
private const val MARGIN_WIDER = "5.0dip"

private const val DEFAULT_ICON = "rounded"

@Suppress("unused")
val overlayButtonsPatch = resourcePatch(
    OVERLAY_BUTTONS.title,
    OVERLAY_BUTTONS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        overlayButtonsBytecodePatch,
        cfBottomUIPatch,
        dismissPlayerHookPatch,
        geminiButton,
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

    val bottomMarginOption = stringOption(
        key = "bottomMargin",
        default = MARGIN_DEFAULT,
        values = mapOf(
            "Default" to MARGIN_DEFAULT,
            "Minimum" to MARGIN_MINIMUM,
            "Wider" to MARGIN_WIDER,
        ),
        title = "Bottom margin",
        description = "The bottom margin for the overlay buttons and timestamp.",
        required = true
    )

    val widerButtonsSpace by booleanOption(
        key = "widerButtonsSpace",
        default = false,
        title = "Wider between-buttons space",
        description = "Prevent adjacent button presses by increasing the horizontal spacing between buttons.",
        required = true
    )

    val changeTopButtons by booleanOption(
        key = "changeTopButtons",
        default = true,
        title = "Change top buttons",
        description = "Change the icons at the top of the player.",
        required = true
    )

    execute {

        // Check patch options first.
        val iconType = iconTypeOption
            .lowerCaseOrThrow()

        var marginBottom = bottomMarginOption
            .lowerCaseOrThrow()

        try {
            val marginBottomFloat = marginBottom.split("dip")[0].toFloat()
            if (marginBottomFloat <= 0f) {
                printWarn("Patch option \"Bottom margin\" must be greater than 0, fallback to minimum.")
                marginBottom = MARGIN_MINIMUM
            }
        } catch (_: Exception) {
            printWarn("Patch option \"Bottom margin\" failed validation, fallback to default.")
            marginBottom = MARGIN_DEFAULT
        }

        val useWiderButtonsSpace = widerButtonsSpace == true

        // Inject hooks for overlay buttons.
        setOf(
            "AlwaysRepeatButton",
            "CopyVideoUrlButton",
            "CopyVideoUrlTimestampButton",
            "ExternalDownloadButton",
            "GeminiButton",
            "LoopSegmentButton",
            "MuteVolumeButton",
            "PlayAllButton",
            "PlaybackSpeedDialogButton",
            "VoiceOverTranslationButton",
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
                "revanced_gemini_copy.xml",
                "revanced_gemini_send.xml",
                "revanced_mute_volume_button.xml",
                "revanced_loop_segment_button.xml",
                "revanced_repeat_button.xml",
                "revanced_vot_button.xml",
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
                    "revanced_loop_segment_button_icon.xml",
                    "revanced_loop_segment_button_start_icon.xml",
                    "revanced_loop_segment_button_active_icon.xml",
                    "revanced_vot_button_icon.xml",
                    "revanced_vot_button_activated_icon.xml",
                    "yt_outline_screen_vertical_vd_theme_24.xml",
                )
            )
        }

        // Subtitle overlay layout for Gemini and Yandex transcription
        copyResources(
            "youtube/overlaybuttons/shared/host",
            ResourceGroup(
                "layout",
                "revanced_subtitle_overlay_layout.xml"
            )
        )

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
                            attribute.textContent = "@+id/revanced_overlay_buttons_scroll_view"
                        }
                    }
            }
        }

        arrayOf(
            "youtube_controls_bottom_ui_container.xml",
            "youtube_controls_fullscreen_button.xml",
            "youtube_controls_cf_fullscreen_button.xml"
        ).forEach { xmlFile ->
            val targetXml = get("res").resolve("layout").resolve(xmlFile)
            if (targetXml.exists()) {
                document("res/layout/$xmlFile").use { document ->
                    document.doRecursively loop@{ node ->
                        if (node !is Element) return@loop

                        // Change the relationship between buttons
                        node.getAttributeNode("yt:layout_constraintRight_toLeftOf")
                            ?.let { attribute ->
                                if (attribute.textContent == "@id/fullscreen_button") {
                                    attribute.textContent =
                                        "@+id/revanced_overlay_buttons_scroll_view"
                                }
                            }

                        node.getAttributeNode("yt:layout_constraintBottom_toTopOf")
                            ?.let { attribute ->
                                if (attribute.textContent == "@id/quick_actions_container") {
                                    attribute.textContent =
                                        "@+id/revanced_overlay_buttons_bottom_margin"
                                }
                            }

                        val (id, height, width) = Triple(
                            node.getAttribute("android:id"),
                            node.getAttribute("android:layout_height"),
                            node.getAttribute("android:layout_width")
                        )
                        val (heightIsNotZero, widthIsNotZero) = Pair(
                            height != "0.0dip",
                            width != "0.0dip",
                        )

                        val isButton =
                            id.endsWith("_button") && id != "@id/multiview_button" || id == "@id/youtube_controls_fullscreen_button_stub"

                        // Adjust TimeBar and Chapter bottom padding
                        val timBarItem = mutableMapOf(
                            "@id/time_bar_chapter_title" to "16.0dip",
                            "@id/timestamps_container" to "14.0dip"
                        )

                        val layoutHeightWidth = if (useWiderButtonsSpace)
                            "56.0dip"
                        else
                            "48.0dip"

                        if (isButton) {
                            node.setAttribute("android:paddingBottom", "12.0dip")
                            node.setAttribute("android:paddingTop", "12.0dip")
                            if (heightIsNotZero && widthIsNotZero) {
                                node.setAttribute("android:layout_height", layoutHeightWidth)
                                node.setAttribute("android:layout_width", layoutHeightWidth)
                            }
                        } else if (timBarItem.containsKey(id)) {
                            if (!useWiderButtonsSpace) {
                                node.setAttribute("android:paddingBottom", timBarItem.getValue(id))
                            }
                        }

                        if (useWiderButtonsSpace && id.endsWith("_placeholder")) {
                            node.setAttribute("android:layout_height", "56.0dip")
                            node.setAttribute("android:layout_width", "56.0dip")
                        }

                        if (id.equals("@+id/revanced_overlay_buttons_bottom_margin")) {
                            node.setAttribute("android:layout_height", marginBottom)
                        } else if (id.equals("@id/time_bar_reference_view")) {
                            node.setAttribute(
                                "yt:layout_constraintBottom_toTopOf",
                                "@id/quick_actions_container"
                            )
                        }
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
