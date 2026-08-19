/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.mapping.ResourceType.COLOR
import app.morphe.patches.shared.mapping.getResourceId
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.misc.fix.proto.parseByteArrayMethodRef
import app.morphe.patches.shared.spoof.guide.addClientOSVersionHook
import app.morphe.patches.shared.spoof.guide.spoofClientGuideEndpointPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PATCHES_PATH
import app.morphe.patches.youtube.utils.navigation.addBottomBarContainerHook
import app.morphe.patches.youtube.utils.navigation.hookNavigationButtonCreated
import app.morphe.patches.youtube.utils.navigation.navigationBarHookPatch
import app.morphe.patches.youtube.utils.patch.PatchList.NAVIGATION_BAR_COMPONENTS
import app.morphe.patches.youtube.utils.playservice.is_19_25_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_28_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_05_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_06_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_21_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_28_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_31_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_46_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.newContentCount
import app.morphe.patches.youtube.utils.resourceid.newContentDot
import app.morphe.patches.youtube.utils.resourceid.searchQuery
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.resourceid.ytOutlineLibrary
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.copyResources
import app.morphe.util.copyXmlNode
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private val navigationBarComponentsResourcePatch = resourcePatch(
    description = "navigationBarComponentsResourcePatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (is_19_28_or_greater) {
            // Since I couldn't get the Cairo notification filled icon anywhere,
            // I just made it as close as possible.
            arrayOf(
                "xxxhdpi",
                "xxhdpi",
                "xhdpi",
                "hdpi",
                "mdpi"
            ).forEach { dpi ->
                copyResources(
                    "youtube/navigationbuttons",
                    ResourceGroup(
                        "drawable-$dpi",
                        "yt_fill_bell_cairo_black_24.png"
                    )
                )
            }

            copyResources(
                "youtube/navigationbuttons",
                ResourceGroup(
                    "drawable-xxxhdpi",
                    "yt_outline_library_cairo_black_24.png"
                )
            )
        }

        copyResources(
            "youtube/navigationbuttons",
            ResourceGroup(
                "layout",
                "empty_content_count.xml",
                "empty_content_dot.xml"
            )
        )

        copyXmlNode(
            "youtube/navigationbuttons/host",
            "layout/image_with_text_tab.xml",
            "android.support.constraint.ConstraintLayout"
        )
    }
}

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/NavigationButtonsPatch;"
private const val THEME_EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/theme/ThemePatch;"

@Suppress("unused")
val navigationBarComponentsPatch = bytecodePatch(
    NAVIGATION_BAR_COMPONENTS.title,
    NAVIGATION_BAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        navigationBarComponentsResourcePatch,
        settingsPatch,
        sharedResourceIdPatch,
        navigationBarHookPatch,
        spoofClientGuideEndpointPatch,
        fixProtoLibraryPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "SETTINGS: HIDE_NAVIGATION_COMPONENTS"
        )

        // region patch for translucent navigation and status bars

        if (is_19_25_or_greater) {
            // Keep the original translucent resources for player controls and only make the
            // color result opaque when it is used for the status bar.
            StatusBarColorFingerprint.method.apply {
                val statusBarColorHook =
                    "$THEME_EXTENSION_CLASS_DESCRIPTOR->getStatusBarColor(I)I"
                insertLiteralOverride(
                    getResourceId(COLOR, "yt_black_pure_opacity60"),
                    statusBarColorHook,
                )
                insertLiteralOverride(
                    getResourceId(COLOR, "yt_white1_opacity70"),
                    statusBarColorHook,
                )
            }

            TranslucentNavigationButtonsFeatureFlagFingerprint.let {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->useTranslucentNavigationButtons(Z)Z",
                )
            }

            TranslucentNavigationButtonsSystemFeatureFlagFingerprint.let {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->useTranslucentNavigationButtons(Z)Z",
                )
            }

            if (is_20_46_or_greater && !is_20_31_or_greater) {
                // Feature interferes with translucent status bar and must be forced off.
                CollapsingToolbarLayoutFeatureFlagFingerprint.let {
                    it.method.insertLiteralOverride(
                        it.instructionMatches.first().index,
                        "$EXTENSION_CLASS_DESCRIPTOR->allowCollapsingToolbarLayout(Z)Z"
                    )
                }
            }

            settingArray += "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS"
            settingArray += "SETTINGS: DISABLE_TRANSLUCENT_STATUS_BAR"
            settingArray += "SETTINGS: DISABLE_TRANSLUCENT_NAVIGATION_BAR"
        }

        // endregion

        // region patch for enable animations for navigation bar

        if (is_20_21_or_greater) {
            AnimatedNavigationTabsFeatureFlagFingerprint.method.insertLiteralOverride(
                AnimatedNavigationTabsFeatureFlagFingerprint.instructionMatches.first().index,
                "$EXTENSION_CLASS_DESCRIPTOR->useAnimatedNavigationButtons(Z)Z"
            )

            settingArray += "SETTINGS: ANIMATED_NAVIGATION_BAR"
        }

        // endregion

        // region patch for enable narrow navigation buttons

        arrayOf(
            PivotBarChangedFingerprint,
            PivotBarStyleFingerprint
        ).forEach { fingerprint ->
            fingerprint.method.apply {
                val targetIndex = fingerprint.instructionMatches.first().index + 1
                val register = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->enableNarrowNavigationButton(Z)Z
                        move-result v$register
                        """
                )
            }
        }

        // endregion

        // region patch for hide navigation bar

        addBottomBarContainerHook("$EXTENSION_CLASS_DESCRIPTOR->hideNavigationBar(Landroid/view/View;)V")

        if (is_20_31_or_greater) {
            AutoHideNavigationBarFingerprint.method.addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->disableAutoHidingNavigationBar()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    :show
                    nop
                """
            )

            settingArray += "SETTINGS: DISABLE_AUTO_HIDE_NAVIGATION_BAR"
        }

        // endregion

        // region add optional navigation buttons

        if (is_20_05_or_greater) {
            PivotBarRendererFingerprint.let {
                it.method.apply {
                    val pivotBarItemRendererType =
                        it.instructionMatches[2].instruction.getReference<TypeReference>()!!.type
                    val constructorIndex = it.instructionMatches[3].index
                    val constructorReference =
                        getInstruction<ReferenceInstruction>(constructorIndex).reference as MethodReference
                    val constructorInstruction =
                        getInstruction<RegisterRangeInstruction>(constructorIndex)
                    val constructorStartRegister = constructorInstruction.startRegister
                    val constructorEndRegister =
                        constructorStartRegister + constructorInstruction.registerCount - 1
                    val messageLiteIndex = constructorReference.parameterTypes.indexOfFirst { parameterType ->
                        parameterType == "Lcom/google/protobuf/MessageLite;"
                    }
                    val messageLiteRegister = constructorStartRegister + messageLiteIndex + 1
                    val insertIndex = it.instructionMatches.last().index
                    val backupRegister =
                        getFreeRegisterProvider(insertIndex, 1).getFreeRegister()

                    addInstructionsAtControlFlowLabel(
                        insertIndex,
                        """
                        # Preserve the original renderer while cloning Home into additional tabs.
                        move-object/16 v$backupRegister, v$messageLiteRegister
                        invoke-static { v$messageLiteRegister }, $EXTENSION_CLASS_DESCRIPTOR->parseSettingsPivotBarItemRenderer(Lcom/google/protobuf/MessageLite;)[B
                        move-result-object v$constructorStartRegister
                        if-eqz v$constructorStartRegister, :ignore_settings

                        sget-object v$messageLiteRegister, $pivotBarItemRendererType->a:$pivotBarItemRendererType
                        invoke-static { v$messageLiteRegister, v$constructorStartRegister }, ${parseByteArrayMethodRef.get()!!}
                        move-result-object v$messageLiteRegister
                        check-cast v$messageLiteRegister, $pivotBarItemRendererType

                        new-instance v$constructorStartRegister, ${constructorReference.definingClass}
                        invoke-direct/range { v$constructorStartRegister .. v$constructorEndRegister }, $constructorReference
                        invoke-static { v$constructorStartRegister }, $EXTENSION_CLASS_DESCRIPTOR->setPivotBarSettingsRenderer(Ljava/lang/Object;)V
                        :ignore_settings

                        move-object/16 v$messageLiteRegister, v$backupRegister
                        invoke-static { v$messageLiteRegister }, $EXTENSION_CLASS_DESCRIPTOR->parseSearchPivotBarItemRenderer(Lcom/google/protobuf/MessageLite;)[B
                        move-result-object v$constructorStartRegister
                        if-eqz v$constructorStartRegister, :ignore_search

                        sget-object v$messageLiteRegister, $pivotBarItemRendererType->a:$pivotBarItemRendererType
                        invoke-static { v$messageLiteRegister, v$constructorStartRegister }, ${parseByteArrayMethodRef.get()!!}
                        move-result-object v$messageLiteRegister
                        check-cast v$messageLiteRegister, $pivotBarItemRendererType

                        new-instance v$constructorStartRegister, ${constructorReference.definingClass}
                        invoke-direct/range { v$constructorStartRegister .. v$constructorEndRegister }, $constructorReference
                        invoke-static { v$constructorStartRegister }, $EXTENSION_CLASS_DESCRIPTOR->setPivotBarSearchRenderer(Ljava/lang/Object;)V
                        :ignore_search

                        move-object/16 v$messageLiteRegister, v$backupRegister
                        nop
                    """
                    )
                }
            }

            PivotBarRendererListFingerprint.let {
                it.method.apply {
                    val insertMatch = it.instructionMatches[2]
                    val insertIndex = insertMatch.index
                    val insertRegister =
                        getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                    val protoListBuilderFingerprint = Fingerprint(
                        accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
                        returnType = insertMatch.instruction.getReference<FieldReference>()!!.type,
                        parameters = listOf("Ljava/util/Collection;")
                    )
                    val protoListBuilderMethod = protoListBuilderFingerprint.method

                    addInstructions(
                        insertIndex,
                        """
                        invoke-static { v$insertRegister }, $EXTENSION_CLASS_DESCRIPTOR->getPivotBarRendererList(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$insertRegister
                        invoke-static { v$insertRegister }, $protoListBuilderMethod
                        move-result-object v$insertRegister
                    """
                    )
                }
            }

            settingArray += "SETTINGS: ANIMATED_NAVIGATION_BAR"
            settingArray += "SETTINGS: SHOW_SEARCH_BUTTON"
            settingArray += "SETTINGS: REORDER_NAVIGATION_BAR"
        }

        // endregion

        // region patch for hide navigation buttons

        // Swap Create and Notifications buttons
        addClientOSVersionHook(
            "patch_setClientOSNameByNavigationBarPatch",
            "$EXTENSION_CLASS_DESCRIPTOR->getOSName()Ljava/lang/String;",
            is_20_06_or_greater,
            true
        )

        TopBarRendererPrimaryFilterFingerprint.let {
            it.method.apply {
                val onClickListenerIndex = it.instructionMatches[3].index
                val onClickListenerRegister =
                    getInstruction<FiveRegisterInstruction>(onClickListenerIndex).registerC

                val copiedButtonRendererIndex = it.instructionMatches[4].index
                val copiedButtonRendererRegister =
                    getInstruction<OneRegisterInstruction>(copiedButtonRendererIndex).registerA

                addInstruction(
                    copiedButtonRendererIndex + 1,
                    "invoke-static { v$copiedButtonRendererRegister, v$onClickListenerRegister }, " +
                            "$EXTENSION_CLASS_DESCRIPTOR->setSearchBarOnClickListener" +
                            $$"(Lcom/google/protobuf/MessageLite;Landroid/view/View$OnClickListener;)V"
                )
            }
        }

        PivotBarBuilderFingerprint.method.apply {
            mapOf(
                newContentCount to "getContentCountId",
                newContentDot to "getContentDotId"
            ).forEach { (literal, methodName) ->
                val literalIndex = indexOfFirstLiteralInstructionOrThrow(literal)
                val viewIndex = indexOfFirstInstructionOrThrow(literalIndex - 1) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "findViewById"
                }
                val viewInstruction = getInstruction<FiveRegisterInstruction>(viewIndex)

                replaceInstruction(
                    viewIndex,
                    "invoke-static {v${viewInstruction.registerC}, v${viewInstruction.registerD}}, " +
                            "$EXTENSION_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;I)Landroid/view/View;"
                )
            }
        }

        ActionBarSearchResultsFingerprint.method.apply {
            val searchQueryId = indexOfFirstLiteralInstructionOrThrow(searchQuery)

            val castIndex = indexOfFirstInstructionOrThrow(searchQueryId) {
                opcode == Opcode.CHECK_CAST &&
                        getReference<TypeReference>()?.type == "Landroid/widget/TextView;"
            }

            val viewRegister = getInstruction<OneRegisterInstruction>(castIndex).registerA

            addInstruction(
                castIndex + 1,
                "invoke-static { v$viewRegister }, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->searchQueryViewLoaded(Landroid/widget/TextView;)V",
            )
        }

        // endregion

        // region patch for hide navigation label

        PivotBarSetTextFingerprint.method.apply {
            val targetIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setText"
            }
            val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

            addInstruction(
                targetIndex,
                "invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
            )
        }

        // endregion

        // region fix for cairo notification icon

        /**
         * The Cairo navigation bar was widely rolled out in YouTube 19.28.42.
         *
         * Unlike Home, Shorts, and Subscriptions, which have Cairo icons,
         * Notifications does not have a Cairo icon.
         *
         * This led to an issue <a href="https://github.com/ReVanced/revanced-patches/issues/4046">revanced-patches#4046</a>,
         * Which was closed as not planned because it was a YouTube issue and not a ReVanced issue.
         *
         * It was not too hard to fix, so it was implemented as a patch.
         */
        if (is_19_28_or_greater && !is_20_28_or_greater) {
            val cairoNotificationEnumReference =
                with(ImageEnumConstructorFingerprint.method) {
                    val stringIndex =
                        indexOfFirstStringInstructionOrThrow(TAB_ACTIVITY_CAIRO_STRING)
                    val cairoNotificationEnumIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                        opcode == Opcode.SPUT_OBJECT
                    }
                    getInstruction<ReferenceInstruction>(cairoNotificationEnumIndex).reference
                }

            SetEnumMapFingerprint.method.apply {
                val enumMapIndex = indexOfFirstInstructionReversedOrThrow {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.definingClass == "Ljava/util/EnumMap;" &&
                            reference.name == "put" &&
                            reference.parameterTypes.firstOrNull() == "Ljava/lang/Enum;"
                }
                val (enumMapRegister, enumRegister) = getInstruction<FiveRegisterInstruction>(
                    enumMapIndex
                ).let {
                    Pair(it.registerC, it.registerD)
                }

                addInstructions(
                    enumMapIndex + 1, """
                        sget-object v$enumRegister, $cairoNotificationEnumReference
                        invoke-static {v$enumMapRegister, v$enumRegister}, $EXTENSION_CLASS_DESCRIPTOR->setCairoNotificationFilledIcon(Ljava/util/EnumMap;Ljava/lang/Enum;)V
                        """
                )
            }

            SetEnumMapSecondaryFingerprint.method.apply {
                val index = indexOfFirstLiteralInstructionOrThrow(ytOutlineLibrary)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getLibraryDrawableId(I)I
                        move-result v$register
                        """
                )
            }
        }

        // endregion

        // Hook navigation button created, in order to hide them.
        hookNavigationButtonCreated(EXTENSION_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, NAVIGATION_BAR_COMPONENTS)

        // endregion
    }
}
