package app.morphe.patches.youtube.general.navigation

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.spoof.guide.addClientOSVersionHook
import app.morphe.patches.shared.spoof.guide.spoofClientGuideEndpointPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.morphe.patches.youtube.utils.navigation.NavigationHook
import app.morphe.patches.youtube.utils.navigation.addBottomBarContainerHook
import app.morphe.patches.youtube.utils.navigation.hookNavigationButtonCreated
import app.morphe.patches.youtube.utils.navigation.navigationBarHookPatch
import app.morphe.patches.youtube.utils.navigation.navigationButtonsMethod
import app.morphe.patches.youtube.utils.patch.PatchList.NAVIGATION_BAR_COMPONENTS
import app.morphe.patches.youtube.utils.playservice.is_19_25_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_28_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_37_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_06_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_28_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.newContentCount
import app.morphe.patches.youtube.utils.resourceid.newContentDot
import app.morphe.patches.youtube.utils.resourceid.searchBox
import app.morphe.patches.youtube.utils.resourceid.searchQuery
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.resourceid.ytOutlineLibrary
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.copyXmlNode
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.indexOfFirstStringInstruction
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
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

private const val EXTENSION_ICON_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/YouTubeIcon;"

@Suppress("unused")
val navigationBarComponentsPatch = bytecodePatch(
    NAVIGATION_BAR_COMPONENTS.title,
    NAVIGATION_BAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        navigationBarComponentsResourcePatch,
        settingsPatch,
        sharedResourceIdPatch,
        navigationBarHookPatch,
        spoofClientGuideEndpointPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "SETTINGS: HIDE_NAVIGATION_COMPONENTS"
        )

        // region patch for enable translucent navigation bar

        if (is_19_25_or_greater) {
            translucentNavigationBarFingerprint.injectLiteralInstructionBooleanCall(
                TRANSLUCENT_NAVIGATION_BAR_FEATURE_FLAG,
                "$EXTENSION_CLASS_DESCRIPTOR->enableTranslucentNavigationBar()Z"
            )

            settingArray += "SETTINGS: TRANSLUCENT_NAVIGATION_BAR"
        }

        // endregion

        // region patch for enable narrow navigation buttons

        arrayOf(
            pivotBarChangedFingerprint,
            pivotBarStyleFingerprint
        ).forEach { fingerprint ->
            fingerprint.matchOrThrow().let {
                it.method.apply {
                    val targetIndex = it.instructionMatches.first().index + 1
                    val register = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->enableNarrowNavigationButton(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for hide navigation bar

        addBottomBarContainerHook("$EXTENSION_CLASS_DESCRIPTOR->hideNavigationBar(Landroid/view/View;)V")

        // endregion

        // region patch for hide navigation buttons

        // Swap Create and Notifications buttons
        addClientOSVersionHook(
            "patch_setClientOSNameByNavigationBarPatch",
            "$EXTENSION_CLASS_DESCRIPTOR->getOSName()Ljava/lang/String;",
            is_20_06_or_greater,
            true
        )

        val onClickListenerFingerprint = if (is_19_37_or_greater)
            searchBarOnClickListenerFingerprint
        else
            searchBarOnClickListenerLegacyFingerprint

        onClickListenerFingerprint.methodOrThrow().apply {
            val searchBoxIdIndex = indexOfFirstLiteralInstructionOrThrow(searchBox)

            val onClickMethodIndex = indexOfFirstInstructionOrThrow(searchBoxIdIndex) {
                val reference = getReference<MethodReference>()
                (opcode == Opcode.INVOKE_VIRTUAL || opcode == Opcode.INVOKE_DIRECT) &&
                        reference?.returnType == "V" &&
                        reference.parameterTypes.size == 2 &&
                        reference.parameterTypes.firstOrNull() == "Landroid/view/View;" &&
                        reference.parameterTypes[1].toString().startsWith("L")
            }

            with (getWalkerMethod(onClickMethodIndex)) {
                val onClickListenerIndex =
                    indexOfFirstInstructionOrThrow(Opcode.NEW_INSTANCE)
                val onClickListenerRegister =
                    getInstruction<OneRegisterInstruction>(onClickListenerIndex).registerA

                val viewIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setOnClickListener"
                }
                val viewRegister =
                    getInstruction<FiveRegisterInstruction>(viewIndex).registerC

                addInstructionsWithLabels(
                    viewIndex, """
                        invoke-static {v$onClickListenerRegister}, $EXTENSION_CLASS_DESCRIPTOR->setSearchBarOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
                        if-eqz v$viewRegister, :ignore
                        """,
                    ExternalLabel("ignore", getInstruction(viewIndex + 1))
                )
            }

            // invoke-direct or invoke-virtual
            val onClickMethodOpcode = getInstruction(onClickMethodIndex).opcode.name
            val onClickMethodReference =
                getInstruction<ReferenceInstruction>(onClickMethodIndex).reference

            val (classRegister, objectRegister) =
                getInstruction<FiveRegisterInstruction>(
                    onClickMethodIndex
                ).let { i -> i.registerC to i.registerE }

            fun getField(register: Int): FieldReference {
                val index =
                    indexOfFirstInstructionReversedOrThrow(onClickMethodIndex) {
                        opcode == Opcode.IGET_OBJECT &&
                                (this as TwoRegisterInstruction).registerA == register
                    }

                return getInstruction<ReferenceInstruction>(index).reference as FieldReference
            }

            fun getParameter(register: Int): Int {
                val index =
                    indexOfFirstInstructionReversedOrThrow(onClickMethodIndex) {
                        opcode.name.startsWith("move-object") &&
                                (this as TwoRegisterInstruction).registerA == register
                    }

                return getInstruction<TwoRegisterInstruction>(index).registerB
            }

            val smaliInstructionsPrefix = if (is_19_37_or_greater) {
                val classField = getField(classRegister)
                val objectField = getField(objectRegister)

                """
                    move-object/from16 v0, p0
                    iget-object v1, v0, $classField
                    iget-object v3, v0, $objectField
                """
            } else {
                val classParameter = getParameter(classRegister)
                val objectParameter = getParameter(objectRegister)

                """
                    move-object/from16 v1, v$classParameter
                    move-object/from16 v3, v$objectParameter
                """
            }

            addInstructions(
                0, smaliInstructionsPrefix + """
                    const/4 v2, 0x0
                    $onClickMethodOpcode {v1, v2, v3}, $onClickMethodReference
                    """
            )
        }

        val enumClass = with(imageEnumConstructorFingerprint.methodOrThrow()) {
            arrayOf(
                SEARCH_STRING to "search",
                SEARCH_CAIRO_STRING to "searchCairo",
            ).map { (enumName, fieldName) ->
                val stringIndex = indexOfFirstStringInstruction(enumName)

                if (stringIndex > -1) {
                    val insertIndex =
                        indexOfFirstInstructionOrThrow(stringIndex, Opcode.SPUT_OBJECT)
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex + 1,
                        "sput-object v$insertRegister, $EXTENSION_ICON_CLASS_DESCRIPTOR->$fieldName:Ljava/lang/Enum;"
                    )
                }
            }

            definingClass
        }

        navigationButtonsMethod.apply {
            findInstructionIndicesReversedOrThrow {
                opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.name == NavigationHook.SET_LAST_APP_NAVIGATION_ENUM.methodName
            }.forEach { enumIndex ->
                val spanIndex = implementation!!.instructions.let {
                    val subListIndex =
                        it.subList(enumIndex, enumIndex + 20).indexOfFirst { instruction ->
                            instruction.opcode == Opcode.INVOKE_STATIC &&
                                    instruction.getReference<MethodReference>()?.returnType == "Landroid/text/Spanned;"
                        } + 1
                    if (subListIndex > 0) {
                        enumIndex + subListIndex
                    } else {
                        -1
                    }
                }

                if (spanIndex > 0) {
                    val spanRegister =
                        getInstruction<OneRegisterInstruction>(spanIndex).registerA

                    addInstructions(
                        spanIndex + 1, """
                            invoke-static {v$spanRegister}, $EXTENSION_CLASS_DESCRIPTOR->changeSpanned(Landroid/text/Spanned;)Landroid/text/Spanned;
                            move-result-object v$spanRegister
                            """
                    )
                }

                val enumRegister =
                    getInstruction<FiveRegisterInstruction>(enumIndex).registerC

                addInstructions(
                    enumIndex + 1, """
                        invoke-static {v$enumRegister}, $EXTENSION_CLASS_DESCRIPTOR->changeIconType(Ljava/lang/Enum;)Ljava/lang/Enum;
                        move-result-object v$enumRegister
                        check-cast v$enumRegister, $enumClass
                        """
                )
            }
        }

        pivotBarBuilderFingerprint.methodOrThrow().apply {
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

        actionBarSearchResultsFingerprint.methodOrThrow().apply {
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

        pivotBarSetTextFingerprint.matchOrThrow().let {
            it.method.apply {
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
                with(imageEnumConstructorFingerprint.methodOrThrow()) {
                    val stringIndex =
                        indexOfFirstStringInstructionOrThrow(TAB_ACTIVITY_CAIRO_STRING)
                    val cairoNotificationEnumIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                        opcode == Opcode.SPUT_OBJECT
                    }
                    getInstruction<ReferenceInstruction>(cairoNotificationEnumIndex).reference
                }

            setEnumMapFingerprint.methodOrThrow().apply {
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

            setEnumMapSecondaryFingerprint.methodOrThrow().apply {
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

