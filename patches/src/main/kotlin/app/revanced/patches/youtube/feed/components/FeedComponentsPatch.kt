package app.revanced.patches.youtube.feed.components

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.mainactivity.onCreateMethod
import app.revanced.patches.youtube.utils.bottomsheet.bottomSheetHookPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.engagementPanelBuilderFingerprint
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.FEED_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.FEED_PATH
import app.revanced.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.youtube.utils.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList.HIDE_FEED_COMPONENTS
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.bar
import app.revanced.patches.youtube.utils.resourceid.captionToggleContainer
import app.revanced.patches.youtube.utils.resourceid.channelListSubMenu
import app.revanced.patches.youtube.utils.resourceid.contentPill
import app.revanced.patches.youtube.utils.resourceid.horizontalCardList
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.scrollTopParentFingerprint
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.fingerprint.injectLiteralInstructionViewCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.util.MethodUtil

private const val CAROUSEL_SHELF_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/CarouselShelfFilter;"
private const val FEED_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/FeedComponentsFilter;"
private const val FEED_VIDEO_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/FeedVideoFilter;"
private const val FEED_VIDEO_VIEWS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/FeedVideoViewsFilter;"
private const val KEYWORD_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/KeywordContentFilter;"
private const val RELATED_VIDEO_CLASS_DESCRIPTOR =
    "$FEED_PATH/RelatedVideoPatch;"

@Suppress("unused")
val feedComponentsPatch = bytecodePatch(
    HIDE_FEED_COMPONENTS.title,
    HIDE_FEED_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        mainActivityResolvePatch,
        navigationBarHookPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        settingsPatch,
        bottomSheetHookPatch,
    )
    execute {

        // region patch for hide carousel shelf, subscriptions channel section, latest videos button

        listOf(
            // carousel shelf, only used to tablet layout.
            Triple(
                breakingNewsFingerprint,
                "hideBreakingNewsShelf",
                horizontalCardList
            ),
            // subscriptions channel section.
            Triple(
                channelListSubMenuFingerprint,
                "hideSubscriptionsChannelSection",
                channelListSubMenu
            ),
            // latest videos button
            Triple(
                contentPillFingerprint,
                "hideLatestVideosButton",
                contentPill
            ),
            Triple(
                latestVideosButtonFingerprint,
                "hideLatestVideosButton",
                bar
            ),
        ).forEach { (fingerprint, methodName, literal) ->
            val smaliInstruction = """
                invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $FEED_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V
                """
            fingerprint.injectLiteralInstructionViewCall(literal, smaliInstruction)
        }

        // endregion

        // region patch for hide caption button

        captionsButtonFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(captionToggleContainer)
            val insertIndex = indexOfFirstInstructionReversedOrThrow(constIndex, Opcode.IF_EQZ)
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $FEED_CLASS_DESCRIPTOR->hideCaptionsButton(Landroid/view/View;)Landroid/view/View;
                    move-result-object v$insertRegister
                    """
            )
        }

        captionsButtonSyntheticFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(captionToggleContainer)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.MOVE_RESULT_OBJECT)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $FEED_CLASS_DESCRIPTOR->hideCaptionsButtonContainer(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for hide floating button

        onCreateMethod.apply {
            val stringIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CONST_STRING &&
                        getReference<StringReference>()?.string == "fab"
            }
            val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA
            val insertIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                opcode == Opcode.INVOKE_DIRECT &&
                        getReference<MethodReference>()?.name == "<init>"
            }
            val jumpIndex = indexOfFirstInstructionOrThrow(insertIndex, Opcode.CONST_STRING)

            addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {v$stringRegister}, $FEED_CLASS_DESCRIPTOR->hideFloatingButton(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$stringRegister
                    if-eqz v$stringRegister, :hide
                    """, ExternalLabel("hide", getInstruction(jumpIndex))
            )
        }

        // endregion

        // region patch for hide relative video

        fun Method.indexOfEngagementPanelBuilderInstruction(targetMethod: MutableMethod) =
            indexOfFirstInstruction {
                opcode == Opcode.INVOKE_DIRECT &&
                        MethodUtil.methodSignaturesMatch(
                            targetMethod,
                            getReference<MethodReference>()!!
                        )
            }

        engagementPanelBuilderFingerprint.matchOrThrow().let {
            it.classDef.methods.filter { method ->
                method.indexOfEngagementPanelBuilderInstruction(it.method) >= 0
            }.forEach { method ->
                method.apply {
                    val index = indexOfEngagementPanelBuilderInstruction(it.method)
                    val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstruction(
                        index + 2,
                        "invoke-static {v$register}, " +
                                "$RELATED_VIDEO_CLASS_DESCRIPTOR->showEngagementPanel(Ljava/lang/Object;)V"
                    )
                }
            }
        }

        engagementPanelUpdateFingerprint.methodOrThrow(engagementPanelBuilderFingerprint)
            .addInstruction(
                0,
                "invoke-static {}, $RELATED_VIDEO_CLASS_DESCRIPTOR->hideEngagementPanel()V"
            )

        linearLayoutManagerItemCountsFingerprint.matchOrThrow().let {
            val methodWalker =
                it.getWalkerMethod(it.patternMatch!!.endIndex)
            methodWalker.apply {
                val index = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $RELATED_VIDEO_CLASS_DESCRIPTOR->overrideItemCounts(I)I
                        move-result v$register
                        """
                )
            }
        }

        // endregion

        // region patch for hide subscriptions channel section for tablet

        arrayOf(
            channelListSubMenuTabletFingerprint,
            channelListSubMenuTabletSyntheticFingerprint
        ).forEach { fingerprint ->
            fingerprint.methodOrThrow().apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $FEED_CLASS_DESCRIPTOR->hideSubscriptionsChannelSection()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
                )
            }
        }

        // endregion

        // region patch for hide category bar

        fun <RegisterInstruction : OneRegisterInstruction> Pair<String, Fingerprint>.patch(
            insertIndexOffset: Int = 0,
            hookRegisterOffset: Int = 0,
            instructions: (Int) -> String
        ) =
            matchOrThrow().let {
                it.method.apply {
                    val endIndex = it.patternMatch!!.endIndex

                    val insertIndex = endIndex + insertIndexOffset
                    val register =
                        getInstruction<RegisterInstruction>(endIndex + hookRegisterOffset).registerA

                    addInstructions(insertIndex, instructions(register))
                }
            }

        filterBarHeightFingerprint.patch<TwoRegisterInstruction> { register ->
            """
                invoke-static { v$register }, $FEED_CLASS_DESCRIPTOR->hideCategoryBarInFeed(I)I
                move-result v$register
            """
        }

        relatedChipCloudFingerprint.patch<OneRegisterInstruction>(1) { register ->
            "invoke-static { v$register }, " +
                    "$FEED_CLASS_DESCRIPTOR->hideCategoryBarInRelatedVideos(Landroid/view/View;)V"
        }

        searchResultsChipBarFingerprint.patch<OneRegisterInstruction>(-1, -2) { register ->
            """
                invoke-static { v$register }, $FEED_CLASS_DESCRIPTOR->hideCategoryBarInSearch(I)I
                move-result v$register
            """
        }

        // endregion

        // region patch for hide mix playlists

        elementParserFingerprint.matchOrThrow(elementParserParentFingerprint).let {
            it.method.apply {
                val freeRegister = implementation!!.registerCount - parameters.size - 2
                val insertIndex = indexOfFirstInstructionOrThrow {
                    val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                    reference?.parameterTypes?.size == 1 &&
                            reference.parameterTypes.first() == "[B" &&
                            reference.returnType.startsWith("L")
                }

                val objectIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_OBJECT)
                val objectRegister = getInstruction<TwoRegisterInstruction>(objectIndex).registerA

                val jumpIndex = it.patternMatch!!.startIndex

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {v$objectRegister, v$freeRegister}, $FEED_COMPONENTS_FILTER_CLASS_DESCRIPTOR->filterMixPlaylists(Ljava/lang/Object;[B)Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :filter
                        """, ExternalLabel("filter", getInstruction(jumpIndex))
                )

                addInstruction(
                    0,
                    "move-object/from16 v$freeRegister, p3"
                )
            }
        }

        // endregion

        // region patch for hide show more button

        showMoreButtonFingerprint.mutableClassOrThrow().let {
            val getViewMethod =
                it.methods.find { method ->
                    method.parameters.isEmpty() &&
                            method.returnType == "Landroid/view/View;"
                }

            getViewMethod?.apply {
                val targetIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $FEED_CLASS_DESCRIPTOR->hideShowMoreButton(Landroid/view/View;)V"
                )
            } ?: throw PatchException("Failed to find getView method")
        }

        // endregion

        // region patch for hide channel tab

        val channelTabBuilderMethod =
            channelTabBuilderFingerprint.methodOrThrow(scrollTopParentFingerprint)

        channelTabRendererFingerprint.matchOrThrow().let {
            it.method.apply {
                val iteratorIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "hasNext"
                }
                val iteratorRegister =
                    getInstruction<FiveRegisterInstruction>(iteratorIndex).registerC

                val targetIndex = indexOfFirstInstructionOrThrow {
                    val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                    opcode == Opcode.INVOKE_INTERFACE &&
                            reference?.returnType == channelTabBuilderMethod.returnType &&
                            reference.parameterTypes == channelTabBuilderMethod.parameterTypes
                }

                val objectIndex =
                    indexOfFirstInstructionReversedOrThrow(targetIndex, Opcode.IGET_OBJECT)
                val objectInstruction = getInstruction<TwoRegisterInstruction>(objectIndex)
                val objectReference = getInstruction<ReferenceInstruction>(objectIndex).reference

                addInstructionsWithLabels(
                    objectIndex + 1, """
                        invoke-static {v${objectInstruction.registerA}}, $FEED_CLASS_DESCRIPTOR->hideChannelTab(Ljava/lang/String;)Z
                        move-result v${objectInstruction.registerA}
                        if-eqz v${objectInstruction.registerA}, :ignore
                        invoke-interface {v$iteratorRegister}, Ljava/util/Iterator;->remove()V
                        goto :next_iterator
                        :ignore
                        iget-object v${objectInstruction.registerA}, v${objectInstruction.registerB}, $objectReference
                        """, ExternalLabel("next_iterator", getInstruction(iteratorIndex))
                )
            }
        }

        // endregion

        addLithoFilter(CAROUSEL_SHELF_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(FEED_COMPONENTS_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(FEED_VIDEO_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(FEED_VIDEO_VIEWS_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(KEYWORD_FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: FEED",
                "SETTINGS: HIDE_FEED_COMPONENTS"
            ),
            HIDE_FEED_COMPONENTS
        )

        // endregion

    }
}
