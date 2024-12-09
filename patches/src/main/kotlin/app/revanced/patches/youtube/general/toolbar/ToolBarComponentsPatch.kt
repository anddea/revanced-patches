package app.revanced.patches.youtube.general.toolbar

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.castbutton.castButtonPatch
import app.revanced.patches.youtube.utils.castbutton.hookToolBarCastButton
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.TOOLBAR_COMPONENTS
import app.revanced.patches.youtube.utils.playservice.is_19_28_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.actionBarRingoBackground
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.voiceSearch
import app.revanced.patches.youtube.utils.resourceid.ytOutlineVideoCamera
import app.revanced.patches.youtube.utils.resourceid.ytPremiumWordMarkHeader
import app.revanced.patches.youtube.utils.resourceid.ytWordMarkHeader
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.getContext
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.toolbar.hookToolBar
import app.revanced.patches.youtube.utils.toolbar.toolBarHookPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.doRecursively
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.replaceLiteralInstructionCall
import app.revanced.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element

@Suppress("unused")
val toolBarComponentsPatch = bytecodePatch(
    TOOLBAR_COMPONENTS.title,
    TOOLBAR_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        castButtonPatch,
        sharedResourceIdPatch,
        settingsPatch,
        toolBarHookPatch,
        versionCheckPatch,
    )

    execute {
        fun MutableMethod.injectSearchBarHook(
            insertIndex: Int,
            insertRegister: Int,
            descriptor: String
        ) =
            addInstructions(
                insertIndex, """
                invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->$descriptor(Z)Z
                move-result v$insertRegister
                """
            )

        fun MutableMethod.injectSearchBarHook(
            insertIndex: Int,
            descriptor: String
        ) =
            injectSearchBarHook(
                insertIndex,
                getInstruction<OneRegisterInstruction>(insertIndex).registerA,
                descriptor
            )

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

        // endregion

        // region patch for enable wide search bar

        // Limitation: Premium header will not be applied for YouTube Premium users if the user uses the 'Wide search bar with header' option.
        // This is because it forces the deprecated search bar to be loaded.
        // As a solution to this limitation, 'Change YouTube header' patch is required.
        actionBarRingoBackgroundFingerprint.methodOrThrow().apply {
            val viewIndex =
                indexOfFirstLiteralInstructionOrThrow(actionBarRingoBackground) + 2
            val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

            addInstructions(
                viewIndex + 1,
                "invoke-static {v$viewRegister}, $GENERAL_CLASS_DESCRIPTOR->setWideSearchBarLayout(Landroid/view/View;)V"
            )

            val targetIndex = indexOfStaticInstruction(this) + 1
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            injectSearchBarHook(
                targetIndex + 1,
                targetRegister,
                "enableWideSearchBarWithHeaderInverse"
            )
        }

        actionBarRingoTextFingerprint.methodOrThrow(actionBarRingoBackgroundFingerprint).apply {
            val targetIndex = indexOfStaticInstruction(this) + 1
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            injectSearchBarHook(
                targetIndex + 1,
                targetRegister,
                "enableWideSearchBarWithHeader"
            )
        }

        actionBarRingoConstructorFingerprint.methodOrThrow().apply {
            val staticCalls = implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val methodReference = (instruction as? ReferenceInstruction)?.reference
                    instruction.opcode == Opcode.INVOKE_STATIC &&
                            methodReference is MethodReference &&
                            methodReference.parameterTypes.size == 1 &&
                            methodReference.returnType == "Z"
                }

            if (staticCalls.size != 2)
                throw PatchException("Size of staticCalls does not match: ${staticCalls.size}")

            mapOf(
                staticCalls.elementAt(0).index to "enableWideSearchBar",
                staticCalls.elementAt(1).index to "enableWideSearchBarWithHeader"
            ).forEach { (index, descriptor) ->
                val walkerMethod = getWalkerMethod(index)

                walkerMethod.apply {
                    injectSearchBarHook(
                        implementation!!.instructions.lastIndex,
                        descriptor
                    )
                }
            }
        }

        youActionBarFingerprint.matchOrThrow(setActionBarRingoFingerprint).let {
            it.method.apply {
                injectSearchBarHook(
                    it.patternMatch!!.endIndex,
                    "enableWideSearchBarInYouTab"
                )
            }
        }

        // This attribution cannot be changed in extension, so change it in the xml file.

        getContext().document("res/layout/action_bar_ringo_background.xml").use { document ->
            document.doRecursively { node ->
                arrayOf("layout_marginStart").forEach replacement@{ replacement ->
                    if (node !is Element) return@replacement

                    node.getAttributeNode("android:$replacement")?.let { attribute ->
                        attribute.textContent = "0.0dip"
                    }
                }
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

        // region patch for hide search term thumbnail

        createSearchSuggestionsFingerprint.methodOrThrow().apply {
            val relativeIndex = indexOfFirstLiteralInstructionOrThrow(40L)
            val replaceIndex = indexOfFirstInstructionReversedOrThrow(relativeIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.toString() == "Landroid/widget/ImageView;->setVisibility(I)V"
            } - 1

            val jumpIndex = indexOfFirstInstructionOrThrow(relativeIndex) {
                opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.toString() == "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;"
            } + 4

            val replaceIndexInstruction = getInstruction<TwoRegisterInstruction>(replaceIndex)
            val replaceIndexReference =
                getInstruction<ReferenceInstruction>(replaceIndex).reference

            addInstructionsWithLabels(
                replaceIndex + 1, """
                    invoke-static { }, $GENERAL_CLASS_DESCRIPTOR->hideSearchTermThumbnail()Z
                    move-result v${replaceIndexInstruction.registerA}
                    if-nez v${replaceIndexInstruction.registerA}, :hidden
                    iget-object v${replaceIndexInstruction.registerA}, v${replaceIndexInstruction.registerB}, $replaceIndexReference
                    """, ExternalLabel("hidden", getInstruction(jumpIndex))
            )
            removeInstruction(replaceIndex)
        }

        // endregion

        /*
        // region patch for hide voice search button

        if (is_19_28_or_greater) {
            imageSearchButtonConfigFingerprint.injectLiteralInstructionBooleanCall(
                45617544L,
                "$GENERAL_CLASS_DESCRIPTOR->hideImageSearchButton(Z)Z"
            )

            updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "ImageSearchButton")

            settingArray += "SETTINGS: HIDE_IMAGE_SEARCH_BUTTON"
        }

        // endregion
         */

        // region patch for hide voice search button

        searchBarFingerprint.matchOrThrow(searchBarParentFingerprint).let {
            it.method.apply {
                val startIndex = it.patternMatch!!.startIndex
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
                val startIndex = indexOfFirstLiteralInstructionOrThrow(voiceSearch)
                val setOnClickListenerIndex = indexOfFirstInstructionOrThrow(startIndex) {
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

        createButtonDrawableFingerprint.methodOrThrow().apply {
            val index = indexOfFirstLiteralInstructionOrThrow(ytOutlineVideoCamera)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                    invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->getCreateButtonDrawableId(I)I
                    move-result v$register
                    """
            )
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
