package app.revanced.patches.music.general.components

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.extension.Constants.GENERAL_PATH
import app.revanced.patches.music.utils.patch.PatchList.HIDE_LAYOUT_COMPONENTS
import app.revanced.patches.music.utils.playservice.is_6_42_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.music.utils.resourceid.musicTasteBuilderShelf
import app.revanced.patches.music.utils.resourceid.playerOverlayChip
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.topBarMenuItemImageView
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.shared.settingmenu.settingsMenuPatch
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
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
        var notificationButtonIncluded = false
        var soundSearchButtonIncluded = false

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
                val targetIndex = it.patternMatch!!.endIndex
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
                    val insertIndex = it.patternMatch!!.startIndex
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
            notificationButtonIncluded = true
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
        if (parentToolMenuFingerprint.resolvable()) {
            parentToolMenuFingerprint.matchOrThrow().let {
                it.method.apply {
                    val index = it.patternMatch!!.startIndex + 1
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

        // region patch for hide sound search button

        if (soundSearchFingerprint.resolvable()) {
            soundSearchFingerprint.injectLiteralInstructionBooleanCall(
                45625491L,
                "$GENERAL_CLASS_DESCRIPTOR->hideSoundSearchButton(Z)Z"
            )
            soundSearchButtonIncluded = true
        }

        // endregion

        // region patch for hide tap to update button

        contentPillFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->hideTapToUpdateButton()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    """, ExternalLabel("show", getInstruction(0))
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
                val insertIndex = it.patternMatch!!.startIndex
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
            "revanced_hide_playlist_card_shelf",
            "false"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_samples_shelf",
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
            "revanced_hide_tap_to_update_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_history_button",
            "false"
        )
        if (notificationButtonIncluded) {
            addSwitchPreference(
                CategoryType.GENERAL,
                "revanced_hide_notification_button",
                "false"
            )
        }
        if (soundSearchButtonIncluded) {
            addSwitchPreference(
                CategoryType.GENERAL,
                "revanced_hide_sound_search_button",
                "false"
            )
        }
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_voice_search_button",
            "false"
        )

        addSwitchPreference(
            CategoryType.SETTINGS,
            "revanced_hide_settings_menu_parent_tools",
            "false",
            false
        )
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
