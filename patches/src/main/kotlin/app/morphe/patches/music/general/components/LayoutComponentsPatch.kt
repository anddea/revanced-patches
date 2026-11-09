/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 * Modified by lavinhoque33, 2026-10-21.
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

package app.morphe.patches.music.general.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.patch.PatchList.HIDE_LAYOUT_COMPONENTS
import app.morphe.patches.music.utils.playservice.is_6_42_or_greater
import app.morphe.patches.music.utils.playservice.is_6_48_or_greater
import app.morphe.patches.music.utils.playservice.is_8_05_or_greater
import app.morphe.patches.music.utils.playservice.is_8_15_or_greater
import app.morphe.patches.music.utils.playservice.is_9_00_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.resourceid.musicTasteBuilderShelf
import app.morphe.patches.music.utils.resourceid.playerOverlayChip
import app.morphe.patches.music.utils.resourceid.searchButton
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.resourceid.topBarMenuItemImageView
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.addTextPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.misc.settings.preference.InputType
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrNull
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.registersUsed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val CUSTOM_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/CustomFilter;"
private const val LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/LayoutComponentsFilter;"

@Suppress("unused")
val layoutComponentsPatch = bytecodePatch(
    HIDE_LAYOUT_COMPONENTS.title,
    HIDE_LAYOUT_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        sharedResourceIdPatch,
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

        // 9.40: offline-tab menu has a different shape (history setVisible is the 3rd call), use fallback fingerprint
        val offlineTabMatch = runCatching { HistoryMenuItemOfflineTabFingerprint.match() }.getOrNull()
        val offlineTabFingerprint = if (offlineTabMatch != null) {
            HistoryMenuItemOfflineTabFingerprint to 2
        } else {
            HistoryMenuItemOfflineTabV940Fingerprint to 4
        }

        arrayOf(
            HistoryMenuItemFingerprint to 1,
            offlineTabFingerprint
        ).forEach { (fingerprint, matchIndex) ->
            fingerprint.method.apply {
                val insertIndex = fingerprint.instructionMatches[matchIndex].index
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

        // 9.40: synthetic bind method has different access flags, fall back to the 9.40 fingerprint
        val tasteBuilderSyntheticMatch = runCatching {
            tasteBuilderSyntheticFingerprint.matchOrNull(tasteBuilderConstructorFingerprint)
        }.getOrNull() ?: tasteBuilderSyntheticV940Fingerprint.matchOrThrow(tasteBuilderConstructorFingerprint)

        tasteBuilderSyntheticMatch.let {
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
        addTextPreference(
            CategoryType.GENERAL,
            "revanced_custom_filter_strings",
            "revanced_custom_filter",
            inputType = InputType.TEXT_MULTI_LINE,
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

        // region hide 'You may also like' section in search
        // Older search pages use a different renderer and do not expose these shelf fields.
        if (is_9_00_or_greater) {
            // Each shelf item holds one renderer: a search history entry, a suggested search,
            // or another renderer used outside the search page.
            val shelfItemsMatches = MusicShelfItemsFingerprint.instructionMatches
            val shelfItemsField = shelfItemsMatches[0].fieldReference
            val searchHistoryField = shelfItemsMatches[2].fieldReference
            val suggestedSearchField = shelfItemsMatches[4].fieldReference
            val shelfType = shelfItemsField.definingClass
            val shelfItemType = searchHistoryField.definingClass
            val protoListType = shelfItemsField.type
            val protoListMutableCopyMethod = Fingerprint(
                definingClass = protoListType,
                returnType = protoListType,
                parameters = listOf("I")
            ).method

            SearchSuggestionsRenderFingerprint.let {
                it.method.apply {
                    val sectionListWrapperInit = it.instructionMatches[1]
                    val sectionListWrapperInitReference = sectionListWrapperInit.getInstruction<ReferenceInstruction>()
                        .reference as MethodReference
                    val sectionListType = sectionListWrapperInitReference.parameterTypes.first().toString()
                    val sectionListRegister = sectionListWrapperInit.instruction.registersUsed[1]

                    // The section list wrapper converts a section list item to its renderer.
                    val sectionListItemType = Fingerprint(
                        definingClass = sectionListWrapperInitReference.definingClass,
                        returnType = "Lj$/util/Optional;",
                        parameters = listOf("L")
                    ).method.parameterTypes.first().toString()

                    val sectionListItemShelfField = classDefBy(sectionListItemType).fields.first { field ->
                        field.type == shelfType
                    }
                    val sectionListContentsFields = classDefBy(sectionListType).fields.filter { field ->
                        !AccessFlags.STATIC.isSet(field.accessFlags) && field.type == protoListType
                    }
                    val recyclerViewField = it.classDef.fields.first { field ->
                        field.type == "Landroid/support/v7/widget/RecyclerView;"
                    }

                    // Returns the list without the shelves that only hold suggested searches.
                    val removeShelvesMethod = ImmutableMethod(
                        definingClass,
                        "patch_removeSuggestedSearchShelves",
                        listOf(ImmutableMethodParameter(protoListType, null, null)),
                        protoListType,
                        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                        null,
                        null,
                        MutableMethodImplementation(10),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                invoke-interface { p0 }, Ljava/util/List;->size()I
                                move-result v0
                                new-instance v1, Ljava/util/ArrayList;
                                invoke-direct { v1, v0 }, Ljava/util/ArrayList;-><init>(I)V
                                const/4 v2, 0x0
                                const/4 v3, 0x0

                                :next_section
                                if-ge v3, v0, :sections_done
                                invoke-interface { p0, v3 }, Ljava/util/List;->get(I)Ljava/lang/Object;
                                move-result-object v4
                                add-int/lit8 v3, v3, 0x1
                                instance-of v5, v4, $sectionListItemType
                                if-eqz v5, :keep_section
                                move-object v5, v4
                                check-cast v5, $sectionListItemType
                                iget-object v5, v5, $sectionListItemShelfField
                                if-eqz v5, :keep_section
                                iget-object v5, v5, $shelfItemsField
                                invoke-interface { v5 }, Ljava/util/List;->iterator()Ljava/util/Iterator;
                                move-result-object v5
                                const/4 v6, 0x0

                                :next_shelf_item
                                invoke-interface { v5 }, Ljava/util/Iterator;->hasNext()Z
                                move-result v7
                                if-eqz v7, :shelf_items_done
                                invoke-interface { v5 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                                move-result-object v7
                                check-cast v7, $shelfItemType
                                iget-object v8, v7, $searchHistoryField
                                if-nez v8, :keep_section
                                iget-object v8, v7, $suggestedSearchField
                                if-eqz v8, :keep_section
                                const/4 v6, 0x1
                                goto :next_shelf_item

                                :shelf_items_done
                                if-eqz v6, :keep_section
                                const/4 v2, 0x1
                                goto :next_section

                                :keep_section
                                invoke-virtual { v1, v4 }, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                                goto :next_section

                                :sections_done
                                if-eqz v2, :unchanged
                                invoke-interface { p0, v0 }, $protoListMutableCopyMethod
                                move-result-object v0
                                invoke-interface { v0 }, Ljava/util/List;->clear()V
                                invoke-interface { v0, v1 }, Ljava/util/List;->addAll(Ljava/util/Collection;)Z
                                return-object v0

                                :unchanged
                                return-object p0
                            """
                        )
                    }.also { method -> it.classDef.methods.add(method) }

                    // Removes the 'You may also like' shelf from the section list, if enabled.
                    val hideSectionMethod = ImmutableMethod(
                        definingClass,
                        "patch_hideYouMayAlsoLikeSection",
                        listOf(
                            ImmutableMethodParameter(definingClass, null, null),
                            ImmutableMethodParameter(sectionListType, null, null)
                        ),
                        "V",
                        AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                iget-object v0, p0, $recyclerViewField
                                invoke-static { v0 }, $LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR->hideYouMayAlsoLikeSection(Landroid/view/View;)Z
                                move-result v0
                                if-eqz v0, :exit
                            """ + sectionListContentsFields.joinToString("") { field ->
                                """
                                iget-object v0, p1, $field
                                invoke-static { v0 }, $removeShelvesMethod
                                move-result-object v0
                                iput-object v0, p1, $field
                                """
                            } + """
                                :exit
                                return-void
                            """
                        )
                    }.also { method -> it.classDef.methods.add(method) }

                    addInstruction(
                        it.instructionMatches[0].index,
                        "invoke-static { p0, v$sectionListRegister }, $hideSectionMethod"
                    )
                }
            }
            addSwitchPreference(
                CategoryType.GENERAL,
                "morphe_music_hide_you_may_also_like_section",
                "false",
                setSummary = false
            )
        }
        // endregion

        addSwitchPreference(
            CategoryType.GENERAL,
            "morphe_music_hide_your_downloads_shelf",
            "false",
            setSummary = false
        )

        updatePatchStatus(HIDE_LAYOUT_COMPONENTS)

    }
}
