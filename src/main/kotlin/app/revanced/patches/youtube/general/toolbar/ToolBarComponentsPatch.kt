package app.revanced.patches.youtube.general.toolbar

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.voicesearch.VoiceSearchUtils.patchXml
import app.revanced.patches.youtube.general.toolbar.fingerprints.ActionBarRingoBackgroundFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.ActionBarRingoTextFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.AttributeResolverFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.CreateButtonDrawableFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.CreateSearchSuggestionsFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.DrawerContentViewConstructorFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.DrawerContentViewFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SearchBarFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SearchBarParentFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SearchResultFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SetActionBarRingoFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.SetWordMarkHeaderFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.TrendingSearchConfigFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.YouActionBarFingerprint
import app.revanced.patches.youtube.utils.castbutton.CastButtonPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ActionBarRingoBackground
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.VoiceSearch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.YtOutlineVideoCamera
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.YtPremiumWordMarkHeader
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.YtWordMarkHeader
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.patches.youtube.utils.toolbar.ToolBarHookPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.doRecursively
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getTargetIndexWithReferenceReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.literalInstructionHook
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element

@Suppress("DEPRECATION", "unused")
object ToolBarComponentsPatch : BaseBytecodePatch(
    name = "Toolbar components",
    description = "Adds options to hide or change components located on the toolbar such as toolbar buttons, search bar, and header.",
    dependencies = setOf(
        CastButtonPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        ToolBarHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        ActionBarRingoBackgroundFingerprint,
        AttributeResolverFingerprint,
        CreateButtonDrawableFingerprint,
        CreateSearchSuggestionsFingerprint,
        DrawerContentViewConstructorFingerprint,
        SearchBarParentFingerprint,
        SearchResultFingerprint,
        SetActionBarRingoFingerprint,
        SetWordMarkHeaderFingerprint,
        TrendingSearchConfigFingerprint
    )
) {
    private const val TARGET_RESOURCE_PATH = "res/layout/action_bar_ringo_background.xml"

    private val ForceHideVoiceSearchButton by booleanPatchOption(
        key = "ForceHideVoiceSearchButton",
        default = false,
        title = "Force hide voice search button",
        description = "Hide voice search button with legacy method, button will always be hidden."
    )

    override fun execute(context: BytecodeContext) {

        // region patch for change YouTube header

        // Invoke YouTube's header attribute into integrations.
        val smaliInstruction = """
            invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->getHeaderAttributeId()I
            move-result v$REGISTER_TEMPLATE_REPLACEMENT
            """

        arrayOf(
            YtPremiumWordMarkHeader,
            YtWordMarkHeader
        ).forEach { literal ->
            context.literalInstructionHook(literal, smaliInstruction)
        }

        // YouTube's headers have the form of AttributeSet, which is decoded from YouTube's built-in classes.
        val attributeResolverMethod = AttributeResolverFingerprint.resultOrThrow().mutableMethod
        val attributeResolverMethodCall = attributeResolverMethod.definingClass + "->" + attributeResolverMethod.name + "(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;"

        context.findClass(GENERAL_CLASS_DESCRIPTOR)!!.mutableClass.methods.single { method ->
            method.name == "getHeaderDrawable"
        }.addInstructions(
            0, """
                invoke-static {p0, p1}, $attributeResolverMethodCall
                move-result-object p0
                return-object p0
                """
        )

        // The sidebar's header is lithoView. Add a listener to change it.
        DrawerContentViewFingerprint.resolve(
            context,
            DrawerContentViewConstructorFingerprint.resultOrThrow().classDef
        )
        DrawerContentViewFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndexWithMethodReferenceName("addView")
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->setDrawerNavigationHeader(Landroid/view/View;)V"
                )
            }
        }

        // Override the header in the search bar.
        val setActionBarRingoMutableClass = SetActionBarRingoFingerprint.resultOrThrow().mutableClass
        setActionBarRingoMutableClass.methods.first {
                method -> MethodUtil.isConstructor(method)
        }.apply {
            val insertIndex = getTargetIndex(Opcode.IPUT_BOOLEAN)
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
        ActionBarRingoBackgroundFingerprint.resultOrThrow().let {
            ActionBarRingoTextFingerprint.resolve(context, it.classDef)
            it.mutableMethod.apply {
                val viewIndex = getWideLiteralInstructionIndex(ActionBarRingoBackground) + 2
                val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

                addInstructions(
                    viewIndex + 1,
                    "invoke-static {v$viewRegister}, $GENERAL_CLASS_DESCRIPTOR->setWideSearchBarLayout(Landroid/view/View;)V"
                )

                val targetIndex = it.scanResult.patternScanResult!!.endIndex + 1
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                injectSearchBarHook(
                    targetIndex + 1,
                    targetRegister,
                    "enableWideSearchBarWithHeaderInverse"
                )
            }

            it.mutableClass.methods.first { method -> MethodUtil.isConstructor(method) }
                .apply {
                    val staticCalls = implementation!!.instructions.withIndex()
                        .filter { instruction ->
                            val methodReference = ((instruction.value as? ReferenceInstruction)?.reference as? MethodReference)
                            methodReference?.parameterTypes?.size == 1 &&
                                    methodReference.returnType == "Z"
                        }

                    if (staticCalls.size != 2)
                        throw PatchException("Size of staticCalls does not match: ${staticCalls.size}")

                    mapOf(
                        staticCalls.elementAt(0).index to "enableWideSearchBar",
                        staticCalls.elementAt(1).index to "enableWideSearchBarWithHeader"
                    ).forEach { (index, descriptor) ->
                        val walkerMethod = getWalkerMethod(context, index)

                        walkerMethod.apply {
                            injectSearchBarHook(
                                implementation!!.instructions.size - 1,
                                descriptor
                            )
                        }
                    }
                }
        }

        ActionBarRingoTextFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex + 1
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                injectSearchBarHook(
                    targetIndex + 1,
                    targetRegister,
                    "enableWideSearchBarWithHeader"
                )
            }
        }

        YouActionBarFingerprint.resolve(context, setActionBarRingoMutableClass)
        YouActionBarFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                injectSearchBarHook(
                    it.scanResult.patternScanResult!!.endIndex,
                    "enableWideSearchBarInYouTab"
                )
            }
        }

        // This attribution cannot be changed in integrations, so change it in the xml file.
        contexts.xmlEditor[TARGET_RESOURCE_PATH].use { editor ->
            editor.file.doRecursively { node ->
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

        CastButtonPatch.hookToolBarButton(context)

        // endregion

        // region patch for hide create button

        ToolBarHookPatch.hook("$GENERAL_CLASS_DESCRIPTOR->hideCreateButton")

        // endregion

        // region patch for hide notification button

        ToolBarHookPatch.hook("$GENERAL_CLASS_DESCRIPTOR->hideNotificationButton")

        // endregion

        // region patch for hide search term thumbnail

        CreateSearchSuggestionsFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val relativeIndex = getWideLiteralInstructionIndex(40)
                val replaceIndex = getTargetIndexWithReferenceReversed(
                    relativeIndex,
                    "Landroid/widget/ImageView;->setVisibility(I)V"
                ) - 1

                val jumpIndex = getTargetIndexWithReference(
                    relativeIndex,
                    "Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;"
                ) + 4

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
        }

        // endregion

        // region patch for hide trending searches

        TrendingSearchConfigFingerprint.literalInstructionBooleanHook(
            45399984,
            "$GENERAL_CLASS_DESCRIPTOR->hideTrendingSearches(Z)Z"
        )

        // endregion

        // region patch for hide voice search button

        if (ForceHideVoiceSearchButton == true) {
            contexts.patchXml(
                arrayOf(
                    "action_bar_search_results_view_mic.xml",
                    "action_bar_search_view.xml",
                    "action_bar_search_view_grey.xml",
                    "action_bar_search_view_mic_out.xml"
                ),
                arrayOf(
                    "height",
                    "marginEnd",
                    "marginStart",
                    "width"
                )
            )
        } else {
            SearchBarFingerprint.resolve(context, SearchBarParentFingerprint.resultOrThrow().classDef)

            SearchBarFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val setVisibilityIndex = getTargetIndexWithMethodReferenceName(startIndex, "setVisibility")
                    val setVisibilityInstruction = getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

                    replaceInstruction(
                        setVisibilityIndex,
                        "invoke-static {v${setVisibilityInstruction.registerC}, v${setVisibilityInstruction.registerD}}, " +
                                "$GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/view/View;I)V"
                    )
                }
            }

            SearchResultFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val startIndex = getWideLiteralInstructionIndex(VoiceSearch)
                    val setOnClickListenerIndex = getTargetIndexWithMethodReferenceName(startIndex, "setOnClickListener")
                    val viewRegister = getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                    addInstruction(
                        setOnClickListenerIndex + 1,
                        "invoke-static {v$viewRegister}, $GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/view/View;)V"
                    )
                }
            }

            /**
             * Add settings
             */
            SettingsPatch.addPreference(
                arrayOf(
                    "SETTINGS: HIDE_VOICE_SEARCH_BUTTON"
                )
            )
        }

        // endregion

        // region patch for replace create button

        CreateButtonDrawableFingerprint.resultOrThrow().mutableMethod.apply {
            val index = getWideLiteralInstructionIndex(YtOutlineVideoCamera)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                    invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->getCreateButtonDrawableId(I)I
                    move-result v$register
                    """
            )
        }

        ToolBarHookPatch.hook("$GENERAL_CLASS_DESCRIPTOR->replaceCreateButton")

        val settingsClass = context.findClass("Shell_SettingsActivity")
            ?: throw PatchException("Shell_SettingsActivity class not found.")

        settingsClass.mutableClass.methods.find { it.name == "onCreate" }?.apply {
            addInstruction(
                0,
                "invoke-static {p0}, $GENERAL_CLASS_DESCRIPTOR->setShellActivityTheme(Landroid/app/Activity;)V"
            )
        } ?: throw PatchException("onCreate method not found.")

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: TOOLBAR_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }

    private fun MutableMethod.injectSearchBarHook(
        insertIndex: Int,
        descriptor: String
    ) {
        injectSearchBarHook(
            insertIndex,
            getInstruction<OneRegisterInstruction>(insertIndex).registerA,
            descriptor
        )
    }

    private fun MutableMethod.injectSearchBarHook(
        insertIndex: Int,
        insertRegister: Int,
        descriptor: String
    ) {
        addInstructions(
            insertIndex, """
                invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->$descriptor(Z)Z
                move-result v$insertRegister
                """
        )
    }
}
