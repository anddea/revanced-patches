package app.morphe.patches.music.general.components

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.music.utils.patch.PatchList.HIDE_LAYOUT_COMPONENTS
import app.morphe.patches.music.utils.playservice.is_6_39_or_greater
import app.morphe.patches.music.utils.playservice.is_6_42_or_greater
import app.morphe.patches.music.utils.playservice.is_6_48_or_greater
import app.morphe.patches.music.utils.playservice.is_8_05_or_greater
import app.morphe.patches.music.utils.playservice.is_8_15_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.resourceid.musicTasteBuilderShelf
import app.morphe.patches.music.utils.resourceid.playerOverlayChip
import app.morphe.patches.music.utils.resourceid.searchButton
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.resourceid.topBarMenuItemImageView
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addPreferenceWithIntent
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.settingmenu.settingsMenuPatch
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_SETTINGS_MENU_DESCRIPTOR =
    "$GENERAL_PATH/SettingsMenuPatch;"
private const val CUSTOM_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/CustomFilter;"
private const val LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/LayoutComponentsFilter;"

@Suppress("unused")
val layoutComponentsPatch = bytecodePatch(
    HIDE_LAYOUT_COMPONENTS.title,
    HIDE_LAYOUT_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        sharedResourceIdPatch,
        settingsMenuPatch,
        versionCheckPatch,
    )

    execute {

        // region patch for hide cast button

        // hide cast button
        mediaRouteButtonFingerprint.mutableClassOrThrow().let {
            val setVisibilityMethod =
                it.methods.find { method -> method.name == "setVisibility" }

            setVisibilityMethod?.addInstructions(
                0, """
                    invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(I)I
                    move-result p1
                    """
            ) ?: throw PatchException("Failed to find setVisibility method")
        }

        // hide floating cast banner
        playerOverlayChipFingerprint.methodOrThrow().apply {
            val targetIndex =
                indexOfFirstLiteralInstructionOrThrow(playerOverlayChip) + 2
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for hide category bar

        chipCloudFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.instructionMatches.last().index
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static { v$targetRegister }, $GENERAL_CLASS_DESCRIPTOR->hideCategoryBar(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide floating button

        floatingButtonFingerprint.methodOrThrow(floatingButtonParentFingerprint).apply {
            addInstructionsWithLabels(
                1, """
                    invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->hideFloatingButton()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    """, ExternalLabel("show", getInstruction(1))
            )
        }

        // endregion

        // region patch for hide history button

        setOf(
            historyMenuItemFingerprint,
            historyMenuItemOfflineTabFingerprint
        ).forEach { fingerprint ->
            fingerprint.matchOrThrow().let {
                it.method.apply {
                    val insertIndex = it.instructionMatches.first().index
                    val insertRegister =
                        getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->hideHistoryButton(Z)Z
                            move-result v$insertRegister
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for hide notification button

        if (is_6_42_or_greater) {
            topBarMenuItemImageViewFingerprint.methodOrThrow().apply {
                val constIndex =
                    indexOfFirstLiteralInstructionOrThrow(topBarMenuItemImageView)
                val targetIndex =
                    indexOfFirstInstructionOrThrow(constIndex, Opcode.MOVE_RESULT_OBJECT)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideNotificationButton(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide setting menus

        preferenceScreenFingerprint.methodOrThrow().apply {
            addInstructions(
                implementation!!.instructions.lastIndex, """
                    invoke-virtual/range {p0 .. p0}, Lcom/google/android/apps/youtube/music/settings/fragment/SettingsHeadersFragment;->getPreferenceScreen()Landroidx/preference/PreferenceScreen;
                    move-result-object v0
                    invoke-static {v0}, $EXTENSION_SETTINGS_MENU_DESCRIPTOR->hideSettingsMenu(Landroidx/preference/PreferenceScreen;)V
                    """
            )
        }

        // The lowest version supported by the patch does not have parent tool settings
        if (is_6_39_or_greater) {
            parentToolMenuFingerprint.matchOrThrow().let {
                it.method.apply {
                    val index = it.instructionMatches.first().index + 1
                    val register = getInstruction<FiveRegisterInstruction>(index).registerD

                    addInstructions(
                        index, """
                            invoke-static {v$register}, $EXTENSION_SETTINGS_MENU_DESCRIPTOR->hideParentToolsMenu(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for hide search button

        searchActionViewFingerprint.methodOrThrow().apply {
            val constIndex =
                indexOfFirstLiteralInstructionOrThrow(searchButton)
            val targetIndex =
                indexOfFirstInstructionOrThrow(constIndex, Opcode.MOVE_RESULT_OBJECT)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideSearchButton(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for hide sound search button

        if (is_6_48_or_greater) {
            if (is_8_05_or_greater) {
                soundSearchFingerprint.methodOrThrow(soundSearchConstructorFingerprint)
                    .addInstructionsWithLabels(
                        0, """
                        invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->hideSoundSearchButton()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        :show
                        nop
                        """
                    )
            } else {
                soundSearchLegacyFingerprint.injectLiteralInstructionBooleanCall(
                    SOUND_SEARCH_BUTTON_FEATURE_FLAG,
                    "$GENERAL_CLASS_DESCRIPTOR->hideSoundSearchButton(Z)Z"
                )
            }
        }

        // endregion

        // region patch for hide tap to update button

        if (!is_8_15_or_greater) {
            contentPillFingerprint
                .methodOrThrow()
                .addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->hideTapToUpdateButton()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        :show
                        nop
                        """
                )
        }

        // endregion

        // region patch for hide taste builder

        tasteBuilderConstructorFingerprint.methodOrThrow().apply {
            val constIndex =
                indexOfFirstLiteralInstructionOrThrow(musicTasteBuilderShelf)
            val targetIndex =
                indexOfFirstInstructionOrThrow(constIndex, Opcode.MOVE_RESULT_OBJECT)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideTasteBuilder(Landroid/view/View;)V"
            )
        }

        tasteBuilderSyntheticFingerprint.matchOrThrow(tasteBuilderConstructorFingerprint).let {
            it.method.apply {
                val insertIndex = it.instructionMatches.first().index
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$insertRegister, 0x0"
                )
            }
        }

        // endregion

        // region patch for hide tooltip content

        tooltipContentViewFingerprint.methodOrThrow().addInstruction(
            0,
            "return-void"
        )

        // endregion

        // region patch for hide voice search button

        searchBarFingerprint.methodOrThrow(searchBarParentFingerprint).apply {
            val setVisibilityIndex = indexOfVisibilityInstruction(this)
            val setVisibilityInstruction =
                getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

            replaceInstruction(
                setVisibilityIndex,
                "invoke-static {v${setVisibilityInstruction.registerC}, v${setVisibilityInstruction.registerD}}, " +
                        "$GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/widget/ImageView;I)V"
            )
        }

        // endregion

        addLithoFilter(CUSTOM_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR)

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_custom_filter",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_custom_filter_strings",
            "revanced_custom_filter"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_button_shelf",
            "false"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_carousel_shelf",
            "false"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_cast_button",
            "true"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_category_bar",
            "false"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_floating_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_history_button",
            "false"
        )
        if (is_6_42_or_greater) {
            addSwitchPreference(
                CategoryType.GENERAL,
                "revanced_hide_notification_button",
                "false"
            )
        }
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_samples_shelf",
            "false"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_search_button",
            "false"
        )
        if (is_6_48_or_greater) {
            addSwitchPreference(
                CategoryType.GENERAL,
                "revanced_hide_sound_search_button",
                "false"
            )
        }
        if (!is_8_15_or_greater) {
            addSwitchPreference(
                CategoryType.GENERAL,
                "revanced_hide_tap_to_update_button",
                "false"
            )
        }
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_voice_search_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_playlist_card_shelf",
            "false"
        )
        if (is_6_39_or_greater) {
            addSwitchPreference(
                CategoryType.SETTINGS,
                "revanced_hide_settings_menu_parent_tools",
                "false",
                false
            )
        }
        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_general",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_playback",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_data_saving",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_downloads_and_storage",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_notification",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_privacy_and_location",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_recommendations",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_paid_memberships",
            "true",
            false
        )
        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_about",
            "false",
            false
        )

        updatePatchStatus(HIDE_LAYOUT_COMPONENTS)

    }
}
