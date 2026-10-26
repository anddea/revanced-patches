/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * https://github.com/MorpheApp/morphe-patches/pull/2451
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

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import app.morphe.patches.youtube.utils.playservice.is_21_35_or_greater
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
import app.morphe.patches.shared.misc.fix.proto.parseByteArrayWithRegistryMethodRef
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
import app.morphe.patches.youtube.utils.playservice.is_20_26_or_greater
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
import app.morphe.util.Utils.printWarn
import app.morphe.util.forEachInlinedFeatureFlagSite
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
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

private const val EXTENSION_SUBSCRIPTIONS_CLASS =
    "Lapp/morphe/extension/youtube/patches/ClassicSubscriptionsLayoutPatch;"

private const val EXTENSION_PIVOT_BAR_ITEM_INTERFACE =
    $$"Lapp/morphe/extension/youtube/patches/ClassicSubscriptionsLayoutPatch$PivotBarItemInterface;"

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
            // The blurred background of the translucent navigation bar draws a copy of the watch layout,
            // so the player surface is drawn twice in every frame, at two positions. Android 12 and 13
            // send the two positions to the surface from two threads at the same time, and the app
            // crashes while the player moves, such as when it is slowly dragged down to the miniplayer
            // (https://github.com/MorpheApp/morphe-patches/pull/3684#issuecomment-6322984709).
            // Android 14 sends them one after the other. On older versions the copy is not drawn,
            // so the bar keeps its translucent color, without the blur.
            NavigationBarBlurDrawFingerprint.let {
                it.method.apply {
                    val index = it.instructionMatches[1].index
                    val hardwareAcceleratedRegister = getInstruction<OneRegisterInstruction>(index).registerA
                    val freeRegisters = getFreeRegisterProvider(index + 1, 2)
                    val sdkRegister = freeRegisters.getFreeRegister4Bit()
                    val minSdkRegister = freeRegisters.getFreeRegister4Bit()

                    addInstructionsWithLabels(
                        index + 1,
                        $$"""
                            sget v$$sdkRegister, Landroid/os/Build$VERSION;->SDK_INT:I
                            const/16 v$$minSdkRegister, 0x22
                            if-ge v$$sdkRegister, v$$minSdkRegister, :draw_blur
                            const/4 v$$hardwareAcceleratedRegister, 0x0
                            :draw_blur
                            nop
                        """
                    )
                }
            }

            if (is_20_21_or_greater) {
                // With the translucent navigation bar, the navigation bar makes room for the system
                // navigation buttons. If the navigation bar is hidden in the Shorts player, nothing makes
                // that room, so the Shorts player makes it itself. The first parameter is the layout
                // of the Shorts player fragment.
                ShortsBottomBarContainerFingerprint.method.apply {
                    addInstruction(
                        0,
                        "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->setShortsPlayerBottomInset(Landroid/view/View;)V"
                    )

                    // The height of the navigation bar is passed to the object that reserves that room below
                    // the Shorts player, right after the view. Other patches can change the height after
                    // it's read, so the room is replaced where it's passed.
                    val getHeightIndex = indexOfFirstInstructionOrThrow {
                        getReference<MethodReference>()?.name == "getHeight"
                    }
                    val reserveRoomIndex = indexOfFirstInstructionOrThrow(getHeightIndex, Opcode.INVOKE_DIRECT_RANGE)
                    val reserveRoomInstruction = getInstruction<RegisterRangeInstruction>(reserveRoomIndex)
                    val parameterTypes = reserveRoomInstruction.getReference<MethodReference>()!!
                        .parameterTypes.map { it.toString() }
                    val heightParameterIndex = parameterTypes.indices.first { index ->
                        parameterTypes[index] == "Landroid/view/View;" && parameterTypes.getOrNull(index + 1) == "I"
                    } + 1
                    // The first register is the instance.
                    val heightRegister = reserveRoomInstruction.startRegister + 1 + heightParameterIndex

                    addInstructions(
                        reserveRoomIndex,
                        """
                        invoke-static/range { v$heightRegister .. v$heightRegister }, $EXTENSION_CLASS_DESCRIPTOR->getShortsPlayerBottomRoom(I)I
                        move-result v$heightRegister
                    """
                    )
                }

                // With the translucent navigation bar, the keyboard shrinks the layout that holds the page
                // and the navigation bar to the top of the keyboard, so the navigation bar is moved above
                // the keyboard. Without it, the keyboard covers the bottom of the page and the navigation bar
                // stays at the bottom of the screen. The keyboard height is replaced with no margin,
                // so the keyboard covers them again.
                PageBottomMarginFingerprint.let {
                    it.method.apply {
                        // The keyboard height and the window inset are put in the same register,
                        // and the window inset is read right after the branch of the keyboard height.
                        val insetIndex = it.instructionMatches.last().index
                        val marginRegister = getInstruction<TwoRegisterInstruction>(insetIndex).registerA
                        val keyboardBranchIndex = indexOfFirstInstructionReversedOrThrow(insetIndex) {
                            opcode == Opcode.IF_EQZ &&
                                    (this as BuilderInstruction21t).target.location.index == insetIndex
                        }
                        val keyboardRegister = getInstruction<OneRegisterInstruction>(keyboardBranchIndex).registerA

                        addInstructionsAtControlFlowLabel(
                            insetIndex + 1,
                            """
                                if-eqz v$keyboardRegister, :keyboard_hidden
                                const/4 v$marginRegister, 0x0
                                :keyboard_hidden
                                nop
                            """
                        )
                    }
                }
            }

            // When the feed is laid out behind the navigation bar, the bottom padding of its list also
            // holds the bottom window inset. With the keyboard open, that inset holds the keyboard too,
            // so the padding grows, while the keyboard leaves the page as it is.
            // While the page loads, as after tapping the search field of the watch history, the list
            // does not scroll and its items only get the height left above the padding. With a tall
            // keyboard nothing is left, and the items are hidden. The keyboard is removed from
            // the padding, as for the navigation bar above.
            if (is_20_26_or_greater) {
                FeedListPaddingFingerprint.let {
                    it.method.apply {
                        val index = it.instructionMatches.last().index
                        val paddingInstruction = getInstruction<FiveRegisterInstruction>(index)
                        val listRegister = paddingInstruction.registerC
                        val bottomPaddingRegister = paddingInstruction.registerG
                        val freeRegisters = getFreeRegisterProvider(index, 3)
                        val insetsRegister = freeRegisters.getFreeRegister4Bit()
                        val imeRegister = freeRegisters.getFreeRegister4Bit()
                        val systemBarsRegister = freeRegisters.getFreeRegister4Bit()

                        addInstructionsWithLabels(
                            index,
                            $$"""
                                # The inset types are only on Android 11 and later.
                                sget v$$imeRegister, Landroid/os/Build$VERSION;->SDK_INT:I
                                const/16 v$$systemBarsRegister, 0x1e
                                if-lt v$$imeRegister, v$$systemBarsRegister, :keep_padding
                                invoke-virtual { v$$listRegister }, Landroid/view/View;->getRootWindowInsets()Landroid/view/WindowInsets;
                                move-result-object v$$insetsRegister
                                if-eqz v$$insetsRegister, :keep_padding
                                # The keyboard inset is empty when the keyboard is hidden.
                                invoke-static { }, Landroid/view/WindowInsets$Type;->ime()I
                                move-result v$$imeRegister
                                invoke-virtual { v$$insetsRegister, v$$imeRegister }, Landroid/view/WindowInsets;->getInsets(I)Landroid/graphics/Insets;
                                move-result-object v$$imeRegister
                                iget v$$imeRegister, v$$imeRegister, Landroid/graphics/Insets;->bottom:I
                                # The padding does not hold the bottom inset, as when the feed is not behind the navigation bar.
                                if-lt v$$bottomPaddingRegister, v$$imeRegister, :keep_padding
                                invoke-static { }, Landroid/view/WindowInsets$Type;->systemBars()I
                                move-result v$$systemBarsRegister
                                invoke-virtual { v$$insetsRegister, v$$systemBarsRegister }, Landroid/view/WindowInsets;->getInsets(I)Landroid/graphics/Insets;
                                move-result-object v$$systemBarsRegister
                                iget v$$systemBarsRegister, v$$systemBarsRegister, Landroid/graphics/Insets;->bottom:I
                                # Keyboard height above the system bars.
                                sub-int/2addr v$$imeRegister, v$$systemBarsRegister
                                if-lez v$$imeRegister, :keep_padding
                                sub-int/2addr v$$bottomPaddingRegister, v$$imeRegister
                                :keep_padding
                                nop
                            """
                        )
                    }
                }
            }

            // Apply translucency to the overlay itself without changing edge-to-edge feature flags.
            addBottomBarContainerHook("$PATCHES_PATH/theme/StatusBarPatch;->apply(Landroid/view/View;)V")

            // The status bar background view keeps the height of the hidden status bar
            // and is drawn over the top bars, making them invisible or clipped.
            StatusBarBackgroundShowFingerprint.matchOrNull(
                StatusBarBackgroundShowFingerprint.classFingerprint!!.originalClassDef
            )?.let {
                it.method.apply {
                    val index = it.instructionMatches.last().index
                    val viewRegister = getInstruction<FiveRegisterInstruction>(index).registerC

                    addInstruction(
                        index + 1,
                        "invoke-static { v$viewRegister }, $PATCHES_PATH/theme/StatusBarPatch;->hideStatusBarBackground(Landroid/view/View;)V"
                    )
                }
            }

            // Keep the original translucent resources for player controls and apply the selected
            // status-bar translucency mode only to the status-bar color result.
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

            // The translucent theme hands the window a transparent color for the system navigation
            // buttons, so an opaque one is swapped in there instead of turning off the feature flag.
            SystemNavigationBarColorFingerprint.let {
                it.method.apply {
                    val colorIndex = it.instructionMatches[1].index
                    val colorRegister = getInstruction<OneRegisterInstruction>(colorIndex).registerA

                    addInstructions(
                        colorIndex + 1,
                        """
                            invoke-static { v$colorRegister }, $EXTENSION_CLASS_DESCRIPTOR->getSystemNavigationBarColor(I)I
                            move-result v$colorRegister
                        """
                    )
                }
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
            settingArray += "SETTINGS: STATUS_BAR_TRANSLUCENCY"
            settingArray += "SETTINGS: DISABLE_TRANSLUCENT_NAVIGATION_BAR"
        }

        // endregion

        // region patch for enable animations for navigation bar

        if (is_20_21_or_greater) {
            // YouTube 21.39 inlines the flag getter into its callers; override each inlined check.
            var animatedTabsHooked = true
            val animatedTabsMatch = AnimatedNavigationTabsFeatureFlagFingerprint.matchOrNull()
            if (animatedTabsMatch != null) {
                animatedTabsMatch.method.insertLiteralOverride(
                    animatedTabsMatch.instructionMatches.first().index,
                    "$EXTENSION_CLASS_DESCRIPTOR->useAnimatedNavigationButtons(Z)Z"
                )
            } else if (forEachInlinedFeatureFlagSite(45680008L) { literalIndex ->
                    insertLiteralOverride(
                        literalIndex,
                        "$EXTENSION_CLASS_DESCRIPTOR->useAnimatedNavigationButtons(Z)Z"
                    )
                } == 0
            ) {
                // The setting is not shown when it has no effect.
                printWarn("\"Enable navigation bar animations\" is not supported in this version. Use YouTube versions up to 21.39.")
                animatedTabsHooked = false
            }

            if (animatedTabsHooked) settingArray += "SETTINGS: ANIMATED_NAVIGATION_BAR"
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
                    if (is_21_35_or_greater) {
                        // Add back the Subscriptions button, if the server moved it to a tab of Home.
                        // The proto is created with the method used for the navigation bar shown
                        // before the guide response is loaded, so the browse endpoint extension is set.
                        // Inserted at the same index as above, so the button is added before the Search
                        // and Settings buttons.
                        val iconType = PivotBarItemProtoFactoryFingerprint.method.parameterTypes[2].toString()
                        // Enum constant names are obfuscated, so the icon type is found by its value.
                        val iconTypeLookupMethod = Fingerprint(
                            definingClass = iconType,
                            returnType = iconType,
                            parameters = listOf("I"),
                            custom = { method, _ ->
                                AccessFlags.STATIC.isSet(method.accessFlags)
                            }
                        ).originalMethod

                        val pivotBarItemOptionalType = PivotBarRendererFingerprint.method.returnType
                        // Non range invoke instructions, so 4-bit registers are required.
                        val freeRegisters = getFreeRegisterProvider(insertIndex, 4,
                            (16 until implementation!!.registerCount).toList() + insertRegister)
                        val browseIdRegister = freeRegisters.getFreeRegister()
                        val labelRegister = freeRegisters.getFreeRegister()
                        val iconRegister = freeRegisters.getFreeRegister()
                        val iconOnlyRegister = freeRegisters.getFreeRegister()

                        check(listOf(browseIdRegister, labelRegister, iconRegister, iconOnlyRegister).all { it < 16 }) {
                            "Subscriptions factory requires four free 4-bit registers"
                        }

                        addInstructionsWithLabels(
                            insertIndex,
                            """
                                # Add the Subscriptions button, if the server removed it.
                                invoke-static { v$insertRegister }, $EXTENSION_SUBSCRIPTIONS_CLASS->needsSubscriptionsPivotBarItem(Ljava/util/List;)Z
                                move-result v$browseIdRegister
                                if-eqz v$browseIdRegister, :skip_subscriptions

                                const-string v$browseIdRegister, "FEsubscriptions"
                                invoke-static { }, $EXTENSION_SUBSCRIPTIONS_CLASS->getSubscriptionsLabel()Ljava/lang/String;
                                move-result-object v$labelRegister
                                invoke-static { }, $EXTENSION_SUBSCRIPTIONS_CLASS->getSubscriptionsIconType()I
                                move-result v$iconRegister
                                invoke-static { v$iconRegister }, $iconTypeLookupMethod
                                move-result-object v$iconRegister
                                const/4 v$iconOnlyRegister, 0x0
                                invoke-static { v$browseIdRegister, v$labelRegister, v$iconRegister, v$iconOnlyRegister }, ${PivotBarItemProtoFactoryFingerprint.method}
                                move-result-object v$browseIdRegister
                                invoke-static { v$browseIdRegister }, ${PivotBarRendererFingerprint.method}
                                move-result-object v$browseIdRegister
                                const/4 v$labelRegister, 0x0
                                invoke-virtual { v$browseIdRegister, v$labelRegister }, $pivotBarItemOptionalType->orElse(Ljava/lang/Object;)Ljava/lang/Object;
                                move-result-object v$browseIdRegister

                                invoke-static { v$insertRegister, v$browseIdRegister }, $EXTENSION_SUBSCRIPTIONS_CLASS->addSubscriptionsPivotBarItem(Ljava/util/List;Ljava/lang/Object;)Ljava/util/List;
                                move-result-object v$insertRegister
                                :skip_subscriptions
                                nop
                            """
                        )
                    }
                }
            }

            settingArray += "SETTINGS: ANIMATED_NAVIGATION_BAR"
            settingArray += "SETTINGS: SHOW_SEARCH_BUTTON"
            settingArray += "SETTINGS: REORDER_NAVIGATION_BAR"
        }

        // endregion

        //
        // Disable the A/B layout with Subscriptions as a tab of Home.
        //

        if (is_21_35_or_greater) {
            // Allow the extension to read the proto of navigation bar items.
            PivotBarRendererFingerprint.classDef.apply {
                val messageLiteField = fields.first { field ->
                    field.type == "Lcom/google/protobuf/MessageLite;"
                }
                // The field can also be PivotBarIconOnlyItemRenderer or other renderers.
                val pivotBarItemRendererType = PivotBarRendererFingerprint.instructionMatches[2]
                    .instruction.getReference<TypeReference>()!!.type

                interfaces.add(EXTENSION_PIVOT_BAR_ITEM_INTERFACE)
                methods.add(
                    ImmutableMethod(
                        type,
                        "patch_getPivotBarItemRenderer",
                        listOf(),
                        "Lcom/google/protobuf/MessageLite;",
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                                iget-object v0, p0, $messageLiteField
                                instance-of v1, v0, $pivotBarItemRendererType
                                if-nez v1, :return
                                const/4 v0, 0x0
                                :return
                                return-object v0
                            """
                        )
                    }
                )
            }

            // Show only the selected tab of the Home and Subscriptions feeds.
            // The tab protos are filtered instead of the browse response,
            // because the response would lose the proto extensions if it's parsed again.
            BrowseResponseTabsFingerprint.let {
                it.method.apply {
                    val streamIndex = it.instructionMatches[1].index
                    val tabsRegister = getInstruction<FiveRegisterInstruction>(streamIndex).registerC

                    addInstructions(
                        streamIndex,
                        """
                            invoke-static { v$tabsRegister }, $EXTENSION_SUBSCRIPTIONS_CLASS->filterBrowseTabs(Ljava/util/List;)Ljava/util/List;
                            move-result-object v$tabsRegister
                        """
                    )
                }
            }

            OfflineNavigationBarSubscriptionsFeatureFlagFingerprint.let {
                it.method.insertLiteralOverride(
                    it.instructionMatches.first().index,
                    "$EXTENSION_SUBSCRIPTIONS_CLASS->hideOfflineSubscriptionsButton(Z)Z"
                )
            }
        }

        //
        // Disable the A/B layout with navigation buttons without labels.
        //

        // Inserted at the start of the method after the other hooks of the method,
        // so the instruction indexes of their matches are still valid.
        PivotBarRendererFingerprint.method.apply {
            val pivotBarItemType = parameterTypes.first().toString()

            addInstructionsWithLabels(
                0,
                """
                    # The parameter register can be higher than 15.
                    invoke-static/range { p0 .. p0 }, $EXTENSION_SUBSCRIPTIONS_CLASS->convertIconOnlyPivotBarItem(Lcom/google/protobuf/MessageLite;)[B
                    move-result-object v0
                    if-eqz v0, :not_icon_only

                    # The endpoints are extensions, which are lost if parsed without the registry.
                    sget-object v1, $pivotBarItemType->a:$pivotBarItemType
                    invoke-static { }, Lcom/google/protobuf/ExtensionRegistryLite;->getGeneratedRegistry()Lcom/google/protobuf/ExtensionRegistryLite;
                    move-result-object v2
                    invoke-static { v1, v0, v2 }, ${parseByteArrayWithRegistryMethodRef.get()!!}
                    move-result-object p0
                    check-cast p0, $pivotBarItemType
                    :not_icon_only
                    nop
                """
            )
        }

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
