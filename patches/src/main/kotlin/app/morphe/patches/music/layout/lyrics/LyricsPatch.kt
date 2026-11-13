/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2269
 * https://github.com/MorpheApp/morphe-patches/pull/3033
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.music.layout.lyrics

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC_THIRD_PARTY_LYRICS
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.patch.PatchList.THIRD_PARTY_LYRICS
import app.morphe.patches.music.utils.playservice.is_9_00_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils
import app.morphe.patches.music.utils.settings.addCustomPreference
import app.morphe.patches.music.utils.settings.addListPreference
import app.morphe.patches.music.utils.settings.addNonInteractivePreference
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.addTextPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.music.video.information.videoInformationPatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.misc.media.MediaSessionSetMetadataFingerprint
import app.morphe.patches.shared.misc.media.MediaSessionSetPlaybackStateFingerprint
import app.morphe.patches.shared.misc.media.hookMediaSessionArgument
import app.morphe.patches.shared.misc.settings.preference.InputType
import app.morphe.util.ResourceGroup
import app.morphe.util.Utils.printWarn
import app.morphe.util.adoptChild
import app.morphe.util.copyResources
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.findMutableClassOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import java.util.logging.Logger
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "Lapp/morphe/extension/music/patches/lyrics/LyricsPatch;"
internal const val PANEL_INSTALLER_CLASS = "Lapp/morphe/extension/music/patches/lyrics/LyricsPanelInstaller;"
private const val LOCKSCREEN_CLASS = "Lapp/morphe/extension/music/patches/lyrics/LockScreenLyrics;"
private const val MINIPLAYER_LYRICS_CLASS = "Lapp/morphe/extension/music/patches/lyrics/MiniPlayerLyrics;"

private const val DEVICE_LYRICS_TAB_CLASS = "Lapp/morphe/extension/music/patches/lyrics/DeviceLyricsTab;"

private const val LYRICS_PANEL_FILTER =
    "Lapp/morphe/extension/music/patches/components/LyricsPanelFilter;"

@Suppress("unused")
val lyricsPatch = bytecodePatch(
    name = "Third-party lyrics",
    description = "Adds an option to show synced lyrics with experience enhancement from 19+ providers in the lyrics panel."
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        lithoFilterPatch,
        videoInformationPatch,
        versionCheckPatch,
        // The copy, romanize, and jump-to-current buttons need their icons whether or not the patches
        // that own these resources are applied.
        resourcePatch {
            execute {
                copyResources(
                    "music/lyrics",
                    ResourceGroup("drawable", "morphe_yt_copy_bold.xml"),
                    ResourceGroup("drawable", "morphe_yt_romanize.xml"),
                    ResourceGroup("drawable", "morphe_yt_crosshair.xml")
                )
            }
        }
    )

    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC_THIRD_PARTY_LYRICS)

    execute {
        if (!is_9_00_or_greater) {
            printWarn("\"${THIRD_PARTY_LYRICS.title}\" is not supported in this version. Use YouTube Music 9.00.00 or later.")
            return@execute
        }

        ResourceUtils.updatePatchStatus(THIRD_PARTY_LYRICS)

        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_enabled",
            "true"
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_source",
            "app.morphe.extension.music.settings.preference.LyricsOrderedListPreference",
            "morphe_music_lyrics_enabled",
            setSummary = false
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_clear_cache",
            "app.morphe.extension.music.settings.preference.LyricsClearCachePreference",
            "morphe_music_lyrics_enabled"
        )
        addTextPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_custom_regex",
            "morphe_music_lyrics_enabled",
            inputType = InputType.TEXT_MULTI_LINE
        )
        addTextPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_text_filter",
            "morphe_music_lyrics_enabled",
            inputType = InputType.TEXT_MULTI_LINE
        )
        addTextPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_credit_line_regex",
            "morphe_music_lyrics_enabled",
            inputType = InputType.TEXT_MULTI_LINE
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_text_size",
            "app.morphe.extension.shared.settings.preference.SliderPreference",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_word_sync",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_spring_scroll",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_gap_dots",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_focus_spotlight",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_show_current_button",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_line_scale",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_split_background",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_force_center",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_allow_transliteration",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_tap_to_seek",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_keep_screen_on",
            "false"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_hide_played",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_hide_unplayed",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_hide_info",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_show_section",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_show_copy_button",
            "true",
            "morphe_music_lyrics_enabled",
            false
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_show_translate_button",
            "true",
            "morphe_music_lyrics_enabled",
            false
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_translation_provider",
            "app.morphe.extension.music.settings.preference.LyricsOrderedListPreference",
            "morphe_music_lyrics_enabled",
            setSummary = false
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_translation_language",
            "app.morphe.extension.music.settings.preference.LyricsTargetLanguagePreference",
            "morphe_music_lyrics_enabled",
            setSummary = false,
            titleKey = null
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_show_romanize_button",
            "true",
            "morphe_music_lyrics_enabled",
            false
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_romanization_provider",
            "app.morphe.extension.music.settings.preference.LyricsOrderedListPreference",
            "morphe_music_lyrics_enabled",
            setSummary = false
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_romanize_language",
            "app.morphe.extension.music.settings.preference.LyricsTargetLanguagePreference",
            "morphe_music_lyrics_enabled",
            setSummary = false,
            titleKey = null
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_dynamic_background",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_dynamic_background_animated",
            "true",
            "morphe_music_lyrics_dynamic_background"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_swap_trans_roma",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_show_refresh_button",
            "true",
            "morphe_music_lyrics_enabled",
            false
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_use_ai_section",
            "app.morphe.extension.music.settings.preference.LyricsAiPromptPreference",
            "morphe_music_lyrics_enabled"
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_offset_ms",
            "app.morphe.extension.shared.settings.preference.SliderPreference",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_sb_matching",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_miniplayer",
            "false",
            "morphe_music_lyrics_enabled",
            false
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_mediasession",
            "false",
            "morphe_music_lyrics_enabled",
            false
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_mini_track_format",
            "app.morphe.extension.music.settings.preference.LyricsTrackFormatPreference",
            "morphe_music_lyrics_enabled"
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_mini_encapsulation",
            "app.morphe.extension.music.settings.preference.LyricsEncapsulationPreference",
            "morphe_music_lyrics_enabled"
        )
        ResourceUtils.addCustomPreference(
            CategoryType.LYRICS.value,
            "morphe_music_lyrics_mini_char_limit",
            "app.morphe.extension.music.settings.preference.LyricsLineLimitsPreference",
            titleKey = "morphe_music_lyrics_mini_line_limits_title",
            summaryKey = "morphe_music_lyrics_mini_line_limits_summary"
        )
        addTextPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_mini_instrumental",
            "morphe_music_lyrics_enabled",
            inputType = InputType.TEXT_MULTI_LINE
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_mini_next_offset_ms",
            "app.morphe.extension.shared.settings.preference.SliderPreference",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_force_capitalize",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_strip_trailing_punctuation",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_han_form",
            "false",
            "morphe_music_lyrics_enabled"
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_about",
            "app.morphe.extension.music.settings.preference.LyricsAboutPreference",
            "morphe_music_lyrics_enabled",
            setSummary = false
        )

        organizeLyricsPreferences()

        // The panel content is built by Elements, so there is no view to hook. The timed
        // lyrics component is the earliest signal that the opened panel is the lyrics one.
        addLithoFilter(LYRICS_PANEL_FILTER)

        // The app builds the watch next response of device files itself, with only a Songs tab,
        // so there is no lyrics tab to show the panel in. A lyrics tab is added to that response,
        // and the page of the tab is replaced with one that only holds the panel. The extension
        // methods filled in here have only their parameter registers.
        fun deviceLyricsTabMethod(name: String) =
            Fingerprint(definingClass = DEVICE_LYRICS_TAB_CLASS, name = name).method

        val tabRendererType = SideloadedWatchNextResponseFingerprint.instructionMatches[1]
            .instruction.getReference<TypeReference>()!!.type

        SideloadedWatchNextResponseFingerprint.let {
            val tabType = it.instructionMatches.last().fieldReference.definingClass
            val messageType = findMutableClassOrThrow(tabType).superclass!!

            it.method.apply {
                val tabDefaultInstance = getInstruction(
                    indexOfFirstInstructionOrThrow {
                        opcode == Opcode.SGET_OBJECT && getReference<FieldReference>()?.let { field ->
                            field.definingClass == tabType && field.type == tabType
                        } == true
                    }
                ).getReference<FieldReference>()!!

                // Only the Songs tab is added, in either of the paths that build the response.
                val addTabIndices = findInstructionIndicesReversedOrThrow(
                    methodCall(parameters = listOf(tabType), returnType = "V")
                )
                val addTabMethod = getInstruction(addTabIndices.first()).getReference<MethodReference>()!!
                addTabIndices.forEach { index ->
                    val builderRegister = getInstruction(index).registersUsed.first()
                    addInstruction(
                        index + 1,
                        "invoke-static/range { v$builderRegister .. v$builderRegister }, " +
                            "$DEVICE_LYRICS_TAB_CLASS->addTab(Ljava/lang/Object;)V"
                    )
                }

                deviceLyricsTabMethod("defaultTab").addInstructions(
                    0,
                    """
                        sget-object v0, $tabDefaultInstance
                        return-object v0
                    """
                )
                deviceLyricsTabMethod("parseTab").addInstructions(
                    0,
                    """
                        check-cast p0, $messageType
                        invoke-static { p0, p1 }, $messageType->parseFrom($messageType[B)$messageType
                        move-result-object p0
                        return-object p0
                    """
                )
                deviceLyricsTabMethod("appendTab").addInstructions(
                    0,
                    """
                        check-cast p0, ${addTabMethod.definingClass}
                        check-cast p1, $tabType
                        invoke-virtual { p0, p1 }, $addTabMethod
                        return-void
                    """
                )
            }
        }

        TabControllerAddTabFingerprint.let {
            val tabbedViewField = it.instructionMatches[1].fieldReference
            val tabModelType = it.method.parameterTypes.first().toString()
            val tabRendererField = tabModelConstructorFingerprint(tabModelType, tabRendererType)
                .instructionMatches.first().fieldReference

            it.method.addInstructions(
                0,
                """
                    iget-object v0, p0, $tabbedViewField
                    invoke-static { v0, p0, p1, p3 }, $DEVICE_LYRICS_TAB_CLASS->onAddTab(Landroid/view/ViewGroup;Ljava/lang/Object;Ljava/lang/Object;Landroid/view/View;)Landroid/view/View;
                    move-result-object p3
                """
            )

            // Like the app, only check the tab renderer for null before casting it.
            deviceLyricsTabMethod("serializeTabRenderer").addInstructionsWithLabels(
                0,
                """
                    check-cast p0, $tabModelType
                    iget-object p0, p0, $tabRendererField
                    if-nez p0, :renderer
                    const/4 p0, 0x0
                    return-object p0
                    :renderer
                    check-cast p0, $tabRendererType
                    invoke-virtual { p0 }, $tabRendererType->toByteArray()[B
                    move-result-object p0
                    return-object p0
                """
            )

            val removeTabMethod = TabControllerRemoveTabFingerprint.originalMethod
            deviceLyricsTabMethod("removeTab").addInstructions(
                0,
                """
                    check-cast p0, ${removeTabMethod.definingClass}
                    check-cast p1, $tabModelType
                    invoke-virtual { p0, p1 }, $removeTabMethod
                    return-void
                """
            )
        }

        // Some accounts get lyrics as a player tab instead of an engagement panel. A tab page can
        // be built long before it is selected, so the litho filter alone can't install the panel.
        TabbedViewTabSelectedFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $PANEL_INSTALLER_CLASS->onTabSelected(Landroid/view/ViewGroup;)V"
        )

        // Some accounts get the panel without a heading, and then nothing in the view tree
        // tells the lyrics panel from the other panels sharing the same container. The panel
        // the app itself put in the container is what tells them apart. Panels with a
        // heading do not need this, so a miss here leaves the rest of the patch working.
        val log = Logger.getLogger(this::class.java.name)
        val engagementPanelType = runCatching {
            EngagementPanelControllerFingerprint.classDef.run {
                val controllerType = type

                // Only instance fields the controller reassigns can hold the panel it shows,
                // and leaving the primitive ones out keeps flags from matching as a type.
                val panelFieldTypes = fields.filter { field ->
                    field.type.startsWith("L") &&
                        !AccessFlags.STATIC.isSet(field.accessFlags) &&
                        !AccessFlags.FINAL.isSet(field.accessFlags)
                }.mapTo(mutableSetOf()) { it.type }

                // The only method taking that type along with a flag is the one that assigns it.
                val setCurrentPanel = methods.single { method ->
                    method.returnType == "V" &&
                        method.parameterTypes.size == 2 &&
                        method.parameterTypes[1].toString() == "Z" &&
                        method.parameterTypes[0].toString() in panelFieldTypes
                }
                val panelType = setCurrentPanel.parameterTypes.first().toString()

                setCurrentPanel.apply {
                    // Hooked after the assignment, because the argument is cleared to null
                    // before it when the panel is being closed rather than shown. The panel
                    // is also handed to other objects here, so the owner is checked too.
                    val assignIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.let { field ->
                                field.type == panelType && field.definingClass == controllerType
                            } == true
                    }
                    addInstruction(
                        assignIndex + 1,
                        "invoke-static { p1 }, " +
                            "$PANEL_INSTALLER_CLASS->onEngagementPanelChanged(Ljava/lang/Object;)V"
                    )
                }

                panelType
            }
        }.onFailure {
            log.warning(
                "Engagement panel controller not found, a lyrics panel shown without a " +
                    "heading will not be used: ${it.message}"
            )
        }.getOrNull()

        // The panel identifier says which panel this is in any language, and with or without
        // a heading. The app reads it with a static helper taking the panel renderer, and the
        // panel hands out its renderer through the only method of the panel type that takes
        // nothing and returns that type.
        if (engagementPanelType != null) {
            val identifierHelper = PanelIdentifierFingerprint.instructionMatches.first().methodReference
            val rendererType = identifierHelper.parameterTypes.single().toString()
            val panelClass = findMutableClassOrThrow(engagementPanelType)
            val rendererGetter = panelClass.methods.single { method ->
                method.parameterTypes.isEmpty() && method.returnType == rendererType
            }
            val invokeGetter = if (AccessFlags.INTERFACE.isSet(panelClass.accessFlags)) {
                "invoke-interface"
            } else {
                "invoke-virtual"
            }

            GetPanelIdentifierFingerprint.method.addInstructionsWithLabels(
                0,
                """
                    check-cast p0, $engagementPanelType
                    $invokeGetter { p0 }, $rendererGetter
                    move-result-object p0
                    if-eqz p0, :no_renderer
                    invoke-static { p0 }, $identifierHelper
                    move-result-object p0
                    return-object p0
                    :no_renderer
                    const/4 p0, 0x0
                    return-object p0
                """
            )
        }

        MediaSessionSetMetadataFingerprint.hookMediaSessionArgument(
            "$EXTENSION_CLASS->onSetMetadata(Landroid/media/MediaMetadata;)V"
        )

        MediaSessionSetMetadataFingerprint.let {
            it.clearMatch()
            val method = it.method
            val index = it.instructionMatches.first().index
            val instruction = method.getInstruction<FiveRegisterInstruction>(index)
            val sessionRegister = instruction.registerC
            val metadataRegister = instruction.registerD
            method.addInstruction(
                index,
                "invoke-static { v$sessionRegister, v$metadataRegister }, " +
                    "$LOCKSCREEN_CLASS->onMediaSessionSetMetadata(Landroid/media/session/MediaSession;Landroid/media/MediaMetadata;)V"
            )
            method.addInstruction(
                index,
                "invoke-static { v$sessionRegister, v$metadataRegister }, " +
                    "$MINIPLAYER_LYRICS_CLASS->onMediaSessionSetMetadata(Landroid/media/session/MediaSession;Landroid/media/MediaMetadata;)V"
            )
        }

        MediaSessionSetPlaybackStateFingerprint.hookMediaSessionArgument(
            "$EXTENSION_CLASS->onSetPlaybackState(Landroid/media/session/PlaybackState;)V"
        )
    }
}

/** Keeps the upstream section order while using this repository's generated settings XML. */
private fun organizeLyricsPreferences() {
    ResourceUtils.editPreferenceCategory(CategoryType.LYRICS.value) {
        val preferences = List(childNodes.length) { childNodes.item(it) }
            .filterIsInstance<Element>()
            .associateBy { it.getAttribute("android:key").removePrefix("morphe_music_lyrics_") }
            .toMutableMap()
        preferences["source"]?.removeAttribute("android:title")
        listOf("source", "translation_provider", "romanization_provider").forEach { key ->
            preferences[key]?.setAttribute("android:selectable", "false")
        }
        adoptChild("Preference") {
            setAttribute("android:key", "morphe_music_lyrics_source_advice")
            setAttribute("android:summary", "@string/morphe_music_lyrics_source_advice_summary")
            setAttribute("android:dependency", "morphe_music_lyrics_enabled")
            setAttribute("android:selectable", "false")
            preferences["source_advice"] = this
        }
        adoptChild("PreferenceCategory") {
            setAttribute("android:key", "morphe_music_lyrics_section_service")
            setAttribute("android:title", "@string/morphe_music_lyrics_source_title")
            listOf(
                "source", "source_advice", "translation_provider", "translation_language",
                "romanization_provider", "romanize_language", "allow_transliteration", "clear_cache"
            ).forEach { key -> preferences[key]?.let { appendChild(it) } }
        }
        adoptChild("PreferenceCategory") {
            setAttribute("android:key", "morphe_music_lyrics_section_display")
            setAttribute("android:title", "@string/morphe_music_lyrics_section_display_title")
            listOf(
                "text_size", "word_sync", "spring_scroll", "gap_dots",
                "focus_spotlight", "line_scale", "split_background", "show_section",
                "use_ai_section", "hide_played", "hide_unplayed", "hide_info",
                "force_capitalize", "strip_trailing_punctuation", "han_form", "swap_trans_roma",
                "force_center"
            ).forEach { key -> preferences[key]?.let { appendChild(it) } }
        }
        adoptChild("PreferenceCategory") {
            setAttribute("android:key", "morphe_music_lyrics_section_panel")
            setAttribute("android:title", "@string/morphe_music_lyrics_section_panel_title")
            listOf(
                "tap_to_seek", "keep_screen_on", "dynamic_background", "dynamic_background_animated",
                "show_copy_button", "show_refresh_button", "show_translate_button", "show_romanize_button",
                "show_current_button"
            ).forEach { key -> preferences[key]?.let { appendChild(it) } }
        }
        adoptChild("PreferenceCategory") {
            setAttribute("android:key", "morphe_music_lyrics_section_sync")
            setAttribute("android:title", "@string/morphe_music_lyrics_section_sync_title")
            listOf(
                "offset_ms", "sb_matching", "miniplayer", "mediasession",
                "mini_track_format", "mini_encapsulation", "mini_char_limit", "mini_instrumental",
                "mini_next_offset_ms"
            ).forEach { key -> preferences[key]?.let { appendChild(it) } }
        }
        adoptChild("PreferenceCategory") {
            setAttribute("android:key", "morphe_settings_music_lyrics_metadata")
            setAttribute("android:title", "@string/morphe_settings_music_lyrics_metadata_title")
            listOf(
                "custom_regex", "text_filter", "credit_line_regex"
            ).forEach { key -> preferences[key]?.let { appendChild(it) } }
        }
        adoptChild("PreferenceCategory") {
            setAttribute("android:key", "morphe_music_lyrics_section_about")
            listOf(
                "about"
            ).forEach { key -> preferences[key]?.let { appendChild(it) } }
        }
    }
}
