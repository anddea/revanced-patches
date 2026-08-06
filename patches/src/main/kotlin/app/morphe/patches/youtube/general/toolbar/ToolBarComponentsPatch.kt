/*
 * Copyright (C) 2024-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
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

/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.toolbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.fix.proto.immutableMethodRef
import app.morphe.patches.shared.misc.fix.proto.mutableCopyMethodRef
import app.morphe.patches.shared.misc.fix.proto.parseByteArrayMethodRef
import app.morphe.patches.youtube.utils.castbutton.castButtonPatch
import app.morphe.patches.youtube.utils.castbutton.hookToolBarCastButton
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.TOOLBAR_COMPONENTS
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_19_16_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_46_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_15_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_31_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.resourceid.ytOutlineExperimentalVideoCamera
import app.morphe.patches.youtube.utils.resourceid.ytOutlineVideoCamera
import app.morphe.patches.youtube.utils.resourceid.ytPremiumWordMarkHeader
import app.morphe.patches.youtube.utils.resourceid.ytWordMarkHeader
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.toolbar.hookToolBar
import app.morphe.patches.youtube.utils.toolbar.toolBarHookPatch
import app.morphe.util.REGISTER_TEMPLATE_REPLACEMENT
import app.morphe.util.containsLiteralInstruction
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.findMethodOrThrow
import app.morphe.util.findMutableMethodOf
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodCall
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstruction
import app.morphe.util.replaceLiteralInstructionCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val NAVIGATION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/NavigationButtonsPatch;"

@Suppress("unused")
val toolBarComponentsPatch = bytecodePatch(
    TOOLBAR_COMPONENTS.title,
    TOOLBAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        castButtonPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        settingsPatch,
        toolBarHookPatch,
        versionCheckPatch,
    )

    execute {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "SETTINGS: TOOLBAR_COMPONENTS"
        )

        // region patch for change YouTube header

        // Invoke YouTube's header attribute into extension.
        val smaliInstruction = """
            invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->getHeaderAttributeId()I
            move-result v$REGISTER_TEMPLATE_REPLACEMENT
            """

        arrayOf(
            ytPremiumWordMarkHeader,
            ytWordMarkHeader
        ).forEach { literal ->
            replaceLiteralInstructionCall(literal, smaliInstruction)
        }

        // YouTube's headers have the form of AttributeSet, which is decoded from YouTube's built-in classes.
        val attributeResolverMethod = attributeResolverFingerprint.methodOrThrow()
        val attributeResolverMethodCall =
            attributeResolverMethod.definingClass + "->" + attributeResolverMethod.name + "(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;"

        findMethodOrThrow(GENERAL_CLASS_DESCRIPTOR) {
            name == "getHeaderDrawable"
        }.addInstructions(
            0, """
                invoke-static {p0, p1}, $attributeResolverMethodCall
                move-result-object p0
                return-object p0
                """
        )

        // The sidebar's header is lithoView. Add a listener to change it.
        drawerContentViewFingerprint.methodOrThrow(drawerContentViewConstructorFingerprint).apply {
            val insertIndex = indexOfAddViewInstruction(this)
            val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

            addInstruction(
                insertIndex,
                "invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->setDrawerNavigationHeader(Landroid/view/View;)V"
            )
        }

        if (!is_20_31_or_greater) {
            // Override the header in the search bar.
            setActionBarRingoFingerprint.mutableClassOrThrow().methods.first { method ->
                MethodUtil.isConstructor(method)
            }.apply {
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IPUT_BOOLEAN)
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "const/4 v$insertRegister, 0x0"
                )
                addInstructions(
                    insertIndex, """
                    invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->overridePremiumHeader()Z
                    move-result v$insertRegister
                    """
                )
            }
        }

        // endregion

        // region patch for hide cast button

        hookToolBarCastButton()

        // endregion

        // region patch for hide create button

        hookToolBar("$GENERAL_CLASS_DESCRIPTOR->hideCreateButton")

        // endregion

        // region patch for hide notification button

        hookToolBar("$GENERAL_CLASS_DESCRIPTOR->hideNotificationButton")

        // endregion

        // region patch for hide search button

        hookToolBar("$GENERAL_CLASS_DESCRIPTOR->hideSearchButton")

        toolbarSearchButtonFingerprint
            .methodOrThrow()
            .apply {
                val index = indexOfShowAsActionInstruction(this)
                val instruction = getInstruction<FiveRegisterInstruction>(index)

                replaceInstruction(
                    index,
                    "invoke-static {v${instruction.registerC}, v${instruction.registerD}}, " +
                            "$GENERAL_CLASS_DESCRIPTOR->hideSearchButton(Landroid/view/MenuItem;I)V"
                )
            }

        // endregion

        // region patch for hide search bar back button

        searchBarParentFingerprint.methodOrThrow().apply {
            addInstruction(
                0,
                "invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->setSearchBarBackButtonActive()V"
            )

            findInstructionIndicesReversedOrThrow {
                opcode == Opcode.RETURN_OBJECT
            }.forEach { returnIndex ->
                val viewRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA

                addInstruction(
                    returnIndex,
                    "invoke-static {v$viewRegister}, $GENERAL_CLASS_DESCRIPTOR->setSearchBarBackButtonView(Landroid/view/View;)V"
                )
            }
        }

        SearchBarBackButtonOnExitFingerprint.method.addInstruction(
            0,
            "invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->clearSearchBarBackButtonView()V"
        )

        SearchBarBackButtonOnResumeFingerprint.match(
            searchBarParentFingerprint.mutableClassOrThrow()
        ).method.apply {
            val insertIndex = indexOfFirstInstructionOrThrow(Opcode.INVOKE_SUPER) + 1

            addInstruction(
                insertIndex,
                "invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->setSearchBarBackButtonActive()V"
            )
        }

        AppCompatToolbarNavigationIconSetterFingerprint.method.apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { returnIndex ->
                addInstruction(
                    returnIndex,
                    "invoke-static {p0, p1}, $GENERAL_CLASS_DESCRIPTOR->applySearchBarBackButtonSpacing(Landroid/view/ViewGroup;Landroid/graphics/drawable/Drawable;)V"
                )
            }
        }

        // endregion

        // region patch for search in channel

        hookToolBar("$GENERAL_CLASS_DESCRIPTOR->openSearchInChannel")

        SearchRequestLoaderFingerprint.method.addInstructions(
            0, """
                invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->overrideSearchInChannelRequestQuery(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
                """
        )

        // endregion

        // region patch for hide search term thumbnail

        if (!is_20_15_or_greater) {
            createSearchSuggestionsFingerprint.methodOrThrow().apply {
                val iteratorIndex = indexOfIteratorInstruction(this)
                val replaceIndex = indexOfFirstInstruction(iteratorIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == "Landroid/widget/ImageView;"
                }
                if (replaceIndex > -1) {
                    val uriIndex = indexOfFirstInstructionOrThrow(replaceIndex) {
                        opcode == Opcode.INVOKE_STATIC &&
                                getReference<MethodReference>()?.toString() == "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;"
                    }
                    val jumpIndex = indexOfFirstInstructionOrThrow(uriIndex, Opcode.CONST_4)
                    val replaceIndexInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
                    val freeRegister = replaceIndexInstruction.registerA
                    val classRegister = replaceIndexInstruction.registerB
                    val replaceIndexReference =
                        getInstruction<ReferenceInstruction>(replaceIndex).reference

                    addInstructionsWithLabels(
                        replaceIndex + 1, """
                    invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->hideSearchTermThumbnail()Z
                    move-result v$freeRegister
                    if-nez v$freeRegister, :hidden
                    iget-object v$freeRegister, v$classRegister, $replaceIndexReference
                    """, ExternalLabel("hidden", getInstruction(jumpIndex))
                    )
                    removeInstruction(replaceIndex)
                } else { // only for YT 20.03
                    val insertIndex = indexOfFirstInstructionOrThrow(iteratorIndex) {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.toString() == "Landroid/widget/ImageView;->setVisibility(I)V"
                    } - 1
                    if (getInstruction(insertIndex).opcode != Opcode.CONST_4) {
                        throw PatchException("Failed to find insert index")
                    }
                    val freeRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA
                    val uriIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                        opcode == Opcode.INVOKE_STATIC &&
                                getReference<MethodReference>()?.toString() == "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;"
                    }
                    val jumpIndex = indexOfFirstInstructionOrThrow(uriIndex, Opcode.CONST_4)

                    addInstructionsWithLabels(
                        insertIndex, """
                        invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->hideSearchTermThumbnail()Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                    )
                }
            }

            if (is_19_16_or_greater) {
                searchFragmentFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                    SEARCH_FRAGMENT_FEATURE_FLAG,
                    "$GENERAL_CLASS_DESCRIPTOR->hideSearchTermThumbnail(Z)Z"
                )
            }

            settingArray += "SETTINGS: HIDE_SEARCH_TERM_THUMBNAIL"
        }

        // endregion

        // region patch for hide voice search button

        searchBarFingerprint.matchOrThrow(searchBarParentFingerprint).let {
            it.method.apply {
                val startIndex = it.instructionMatches.first().index
                val setVisibilityIndex = indexOfFirstInstructionOrThrow(startIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setVisibility"
                }
                val setVisibilityInstruction =
                    getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

                replaceInstruction(
                    setVisibilityIndex,
                    "invoke-static {v${setVisibilityInstruction.registerC}, v${setVisibilityInstruction.registerD}}, " +
                            "$GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/view/View;I)V"
                )
            }
        }

        searchResultFingerprint.matchOrThrow().let {
            it.method.apply {
                val voiceInputControllerActivityMethodCall =
                    voiceInputControllerFingerprint
                        .methodOrThrow(voiceInputControllerParentFingerprint)
                        .methodCall()

                val voiceInputControllerActivityIndex =
                    indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.toString() == voiceInputControllerActivityMethodCall
                    }
                val setOnClickListenerIndex =
                    indexOfFirstInstructionOrThrow(voiceInputControllerActivityIndex) {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.name == "setOnClickListener"
                    }
                val viewRegister =
                    getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                addInstruction(
                    setOnClickListenerIndex + 1,
                    "invoke-static {v$viewRegister}, $GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide You may like section

        if (is_20_15_or_greater) {
            val searchSuggestionEndpointField = SearchSuggestionEndpoint2021Fingerprint
                .instructionMatches.first().instruction.getReference<FieldReference>()!!
            val searchSuggestionEndpointClass = searchSuggestionEndpointField.definingClass

            SearchBoxTypingStringFingerprint.let {
                it.method.apply {
                    // Includes trending searches ("You may like") and search history.
                    val searchSuggestionCollectionField =
                        it.instructionMatches.first().instruction.getReference<FieldReference>()!!
                    val typedStringField =
                        it.instructionMatches[2].instruction.getReference<FieldReference>()!!

                    val helperMethod = ImmutableMethod(
                        definingClass,
                        "patch_setSearchSuggestions",
                        listOf(
                            ImmutableMethodParameter(
                                parameterTypes.first().toString(),
                                null,
                                null,
                            ),
                        ),
                        "V",
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(7),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                move-object/from16 v0, p1
                                iget-object v1, v0, $typedStringField

                                # Filter only while the setting is enabled and the query is empty.
                                invoke-static {v1}, $GENERAL_CLASS_DESCRIPTOR->hideYouMayLikeSection(Ljava/lang/String;)Z
                                move-result v1
                                if-eqz v1, :ignore

                                iget-object v1, v0, $searchSuggestionCollectionField
                                invoke-interface {v1}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;
                                move-result-object v2

                                :loop
                                invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z
                                move-result v3
                                if-eqz v3, :exit
                                invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                                move-result-object v3
                                instance-of v4, v3, $searchSuggestionEndpointClass
                                if-eqz v4, :loop
                                check-cast v3, $searchSuggestionEndpointClass
                                iget-object v4, v3, $searchSuggestionEndpointField
                                invoke-static {v3, v4}, $GENERAL_CLASS_DESCRIPTOR->isSearchHistory(Ljava/lang/Object;Ljava/lang/String;)Z
                                move-result v3
                                if-nez v3, :loop
                                invoke-interface {v2}, Ljava/util/Iterator;->remove()V
                                goto :loop

                                :exit
                                iput-object v1, v0, $searchSuggestionCollectionField

                                :ignore
                                return-void
                            """,
                        )
                    }

                    it.classDef.methods.add(helperMethod)
                    addInstruction(
                        0,
                        "invoke-direct/range {p0 .. p1}, $helperMethod",
                    )
                }
            }

            settingArray += "SETTINGS: HIDE_YOU_MAY_LIKE_SECTION"
        } else if (is_19_46_or_greater && !is_20_15_or_greater) {
            val (searchSuggestionEndpointClass, searchSuggestionEndpointField) = with(
                searchSuggestionEndpointFingerprint.methodOrThrow(
                    searchSuggestionEndpointParentFingerprint
                )
            ) {
                val isEmptyIndex = indexOfIsEmptyInstruction(this)
                val index = indexOfFirstInstructionReversedOrThrow(isEmptyIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == "Ljava/lang/String;"
                }
                val searchSuggestionEndpointField =
                    getInstruction<ReferenceInstruction>(index).reference as FieldReference

                Pair(
                    searchSuggestionEndpointField.definingClass,
                    searchSuggestionEndpointField
                )
            }

            searchSuggestionCollectionFingerprint.matchOrThrow(
                createSearchSuggestionsFingerprint
            ).let {
                it.method.apply {
                    val helperMethodName = "patch_setCollection"

                    it.classDef.methods.add(
                        ImmutableMethod(
                            it.classDef.type,
                            helperMethodName,
                            listOf(
                                ImmutableMethodParameter(
                                    "Ljava/util/Collection;",
                                    annotations,
                                    "collection"
                                ),
                                ImmutableMethodParameter(
                                    "Ljava/lang/String;",
                                    annotations,
                                    "searchQuery"
                                )
                            ),
                            "Ljava/util/Collection;",
                            AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                            annotations,
                            null,
                            MutableMethodImplementation(8),
                        ).toMutable().apply {
                            addInstructionsWithLabels(
                                0,
                                """
                                    # Collection.
                                    move-object/from16 v0, p1
                                    # Search query.
                                    move-object/from16 v1, p2
                                    
                                    # Check that the setting is enabled and that the search query is empty.
                                    invoke-static {v1}, $GENERAL_CLASS_DESCRIPTOR->hideYouMayLikeSection(Ljava/lang/String;)Z
                                    move-result v2

                                    if-eqz v2, :exit
                                    
                                    invoke-interface {v0}, Ljava/util/Collection;->iterator()Ljava/util/Iterator;
                                    move-result-object v2
                                    
                                    :loop
                                    invoke-interface {v2}, Ljava/util/Iterator;->hasNext()Z
                                    move-result v3
                                    
                                    if-eqz v3, :exit
                                    
                                    invoke-interface {v2}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                                    move-result-object v3
                                    instance-of v4, v3, $searchSuggestionEndpointClass

                                    if-eqz v4, :loop
                                    check-cast v3, $searchSuggestionEndpointClass
                                    iget-object v4, v3, $searchSuggestionEndpointField
                                    invoke-static {v3, v4}, $GENERAL_CLASS_DESCRIPTOR->isSearchHistory(Ljava/lang/Object;Ljava/lang/String;)Z
                                    move-result v3

                                    if-nez v3, :loop
                                    
                                    # If it's not a search history, it's a search term suggestion.
                                    # Remove it from the collection.
                                    invoke-interface {v2}, Ljava/util/Iterator;->remove()V
                                    goto :loop

                                    :exit
                                    return-object v0
                                    """,
                            )
                        }
                    )

                    addInstructions(
                        0, """
                            invoke-direct/range {p0 .. p2}, $definingClass->$helperMethodName(Ljava/util/Collection;Ljava/lang/String;)Ljava/util/Collection;
                            move-result-object v0
                            move-object/from16 p1, v0
                            """
                    )
                }
            }

            roundEdgeSearchBarFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                ROUND_EDGE_SEARCH_BAR_FEATURE_FLAG,
                "$GENERAL_CLASS_DESCRIPTOR->disableRoundSearchBar(Z)Z"
            )

            settingArray += "SETTINGS: HIDE_YOU_MAY_LIKE_SECTION"
        }

        hookToolBar("$NAVIGATION_CLASS_DESCRIPTOR->setToolbarSettingsOnClickListener")

        TopBarRendererSecondaryFilterFingerprint.let {
            it.method.apply {
                var buttonsClass: String? = null
                val protoListIndex = it.instructionMatches.first().index
                for (index in protoListIndex until implementation!!.instructions.size) {
                    val instruction = getInstruction(index)
                    if (instruction.opcode == Opcode.CHECK_CAST) {
                        buttonsClass =
                            (instruction as ReferenceInstruction).reference.toString()
                        break
                    }
                }

                val protoListRegister =
                    getInstruction<FiveRegisterInstruction>(protoListIndex).registerC
                val freeRegisters = getFreeRegisterProvider(protoListIndex, 2)
                val protoListFreeRegister = freeRegisters.getFreeRegister()
                val buttonByteRegister = freeRegisters.getFreeRegister()

                addInstructionsWithLabels(
                    protoListIndex,
                    """
                            invoke-interface {v$protoListRegister}, ${immutableMethodRef.get()}
                            move-result v$protoListFreeRegister
                            if-nez v$protoListFreeRegister, :immutable

                            invoke-static {v$protoListRegister}, ${mutableCopyMethodRef.get()}
                            move-result-object v$protoListRegister

                            invoke-static {v$protoListRegister}, $NAVIGATION_CLASS_DESCRIPTOR->createToolbarSettingsButton(Ljava/util/List;)[B
                            move-result-object v$buttonByteRegister
                            if-eqz v$buttonByteRegister, :settings_button_not_created

                            sget-object v$protoListFreeRegister, $buttonsClass->a:$buttonsClass
                            invoke-static {v$protoListFreeRegister, v$buttonByteRegister}, ${parseByteArrayMethodRef.get()!!}
                            move-result-object v$protoListFreeRegister
                            check-cast v$protoListFreeRegister, $buttonsClass
                            invoke-interface {v$protoListRegister, v$protoListFreeRegister}, Ljava/util/List;->add(Ljava/lang/Object;)Z
                            invoke-static {v$protoListRegister}, $NAVIGATION_CLASS_DESCRIPTOR->applyToolbarSettingsButtonIndex(Ljava/util/List;)V

                            :settings_button_not_created
                            invoke-static {v$protoListRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceToolbarCreateButton(Ljava/util/List;)[B
                            move-result-object v$buttonByteRegister
                            if-eqz v$buttonByteRegister, :immutable

                            sget-object v$protoListFreeRegister, $buttonsClass->a:$buttonsClass
                            invoke-static {v$protoListFreeRegister, v$buttonByteRegister}, ${parseByteArrayMethodRef.get()!!}
                            move-result-object v$protoListFreeRegister
                            check-cast v$protoListFreeRegister, $buttonsClass
                            invoke-static {}, $NAVIGATION_CLASS_DESCRIPTOR->getToolbarCreateButtonIndex()I
                            move-result v$buttonByteRegister
                            invoke-interface {v$protoListRegister, v$buttonByteRegister, v$protoListFreeRegister}, Ljava/util/List;->set(ILjava/lang/Object;)Ljava/lang/Object;

                            :immutable
                            nop
                        """,
                )
            }
        }

        settingArray += "SETTINGS: SHOW_TOOLBAR_SETTINGS_BUTTON"

        // endregion

        // region patch for hide YouTube Doodles

        yoodlesImageViewFingerprint.methodOrThrow().apply {
            findInstructionIndicesReversedOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL
                        && getReference<MethodReference>()?.name == "setImageDrawable"
            }.forEach { insertIndex ->
                val (viewRegister, drawableRegister) = getInstruction<FiveRegisterInstruction>(
                    insertIndex
                ).let {
                    Pair(it.registerC, it.registerD)
                }
                replaceInstruction(
                    insertIndex,
                    "invoke-static {v$viewRegister, v$drawableRegister}, " +
                            "$GENERAL_CLASS_DESCRIPTOR->hideYouTubeDoodles(Landroid/widget/ImageView;Landroid/graphics/drawable/Drawable;)V"
                )
            }
        }

        // endregion

        // region patch for replace create button

        val matchedMethods = mutableListOf<MutableMethod>()
        classDefForEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.containsLiteralInstruction(ytOutlineVideoCamera)) {
                    val mutableMethod = mutableClassDefBy(classDef).findMutableMethodOf(method)
                    matchedMethods.add(mutableMethod)
                }
            }
        }

        if (matchedMethods.isEmpty()) {
            throw PatchException("No methods matched createButtonDrawableFingerprint")
        }

        // println("Found ${matchedMethods.size} methods matching createButtonDrawableFingerprint")
        // matchedMethods.forEach { method ->
        //     println("Patching method: ${method.methodCall()} in class ${method.definingClass}")
        // }

        matchedMethods.forEach { method ->
            method.apply {
                val indices = mutableListOf<Int>()

                val idx1 = indexOfFirstLiteralInstruction(ytOutlineVideoCamera)
                if (idx1 != -1) indices.add(idx1)

                val idx2 = indexOfFirstLiteralInstruction(ytOutlineExperimentalVideoCamera)
                if (idx2 != -1) indices.add(idx2)

                // Sort descending so we modify the end of the method first,
                // preventing index shifting from affecting subsequent inserts
                indices.sortedDescending().forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA
                    addInstructions(
                        index + 1, """
                        invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->getCreateButtonDrawableId(I)I
                        move-result v$register
                        """
                    )
                }
            }
        }

        hookToolBar("$GENERAL_CLASS_DESCRIPTOR->replaceCreateButton")

        findMethodOrThrow(
            "Lcom/google/android/apps/youtube/app/application/Shell_SettingsActivity;"
        ) {
            name == "onCreate"
        }.addInstruction(
            0,
            "invoke-static {p0}, $GENERAL_CLASS_DESCRIPTOR->setShellActivityTheme(Landroid/app/Activity;)V"
        )

        // endregion

        // region add settings

        addPreference(
            settingArray,
            TOOLBAR_COMPONENTS
        )

        // endregion

    }
}
