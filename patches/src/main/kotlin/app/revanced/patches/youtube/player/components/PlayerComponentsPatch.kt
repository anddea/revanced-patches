package app.revanced.patches.youtube.player.components

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.shared.spans.addSpanFilter
import app.revanced.patches.shared.spans.inclusiveSpanPatch
import app.revanced.patches.shared.startVideoInformerFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.controlsoverlay.controlsOverlayConfigPatch
import app.revanced.patches.youtube.utils.engagementPanelBuilderFingerprint
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.SPANS_PATH
import app.revanced.patches.youtube.utils.fix.suggestedvideoendscreen.suggestedVideoEndScreenPatch
import app.revanced.patches.youtube.utils.patch.PatchList.PLAYER_COMPONENTS
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.darkBackground
import app.revanced.patches.youtube.utils.resourceid.fadeDurationFast
import app.revanced.patches.youtube.utils.resourceid.scrimOverlay
import app.revanced.patches.youtube.utils.resourceid.seekUndoEduOverlayStub
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.tapBloomView
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.youtubeControlsOverlayFingerprint
import app.revanced.patches.youtube.video.information.hookVideoInformation
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.injectLiteralInstructionViewCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val speedOverlayPatch = bytecodePatch(
    description = "speedOverlayPatch"
) {
    dependsOn(sharedResourceIdPatch)

    execute {
        fun MutableMethod.hookSpeedOverlay(
            insertIndex: Int,
            insertRegister: Int,
            jumpIndex: Int
        ) {
            addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay()Z
                    move-result v$insertRegister
                    if-eqz v$insertRegister, :disable
                    """, ExternalLabel("disable", getInstruction(jumpIndex))
            )
        }

        val resolvable = restoreSlideToSeekBehaviorFingerprint.resolvable() &&
                speedOverlayFingerprint.resolvable() &&
                speedOverlayFloatValueFingerprint.resolvable()

        if (resolvable) {
            // Used on YouTube 18.29.38 ~ YouTube 19.17.41

            // region patch for Disable speed overlay (Enable slide to seek)

            mapOf(
                restoreSlideToSeekBehaviorFingerprint to 45411329L,
                speedOverlayFingerprint to 45411330L
            ).forEach { (fingerprint, literal) ->
                fingerprint.injectLiteralInstructionBooleanCall(
                    literal,
                    "$PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay(Z)Z"
                )
            }

            // endregion

            // region patch for Custom speed overlay float value

            speedOverlayFloatValueFingerprint.matchOrThrow().let {
                it.method.apply {
                    val index = it.patternMatch!!.startIndex
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    addInstructions(
                        index + 1, """
                        invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue(F)F
                        move-result v$register
                        """
                    )
                }
            }

            // endregion

        } else {
            // Used on YouTube 19.18.41~

            // region patch for Disable speed overlay (Enable slide to seek)

            nextGenWatchLayoutFingerprint.methodOrThrow().apply {
                val booleanValueIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "booleanValue"
                }
                val insertIndex = indexOfFirstInstructionOrThrow(booleanValueIndex - 10) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.definingClass == definingClass
                }
                val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                val insertReference = getInstruction<ReferenceInstruction>(insertIndex).reference

                addInstruction(
                    insertIndex + 1,
                    "iget-object v${insertInstruction.registerA}, v${insertInstruction.registerB}, $insertReference"
                )

                val jumpIndex = indexOfFirstInstructionOrThrow(booleanValueIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.definingClass == definingClass
                }

                hookSpeedOverlay(insertIndex + 1, insertInstruction.registerA, jumpIndex)
            }

            val (slideToSeekBooleanMethod, slideToSeekSyntheticMethod) =
                slideToSeekMotionEventFingerprint.matchOrThrow(
                    horizontalTouchOffsetConstructorFingerprint
                ).let {
                    with(it.method) {
                        val patternMatch = it.patternMatch!!
                        val jumpIndex = patternMatch.endIndex + 1
                        val insertIndex = patternMatch.endIndex - 1
                        val insertRegister =
                            getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                        hookSpeedOverlay(insertIndex, insertRegister, jumpIndex)

                        val slideToSeekBooleanMethod =
                            getWalkerMethod(patternMatch.startIndex + 1)

                        val slideToSeekConstructorMethod =
                            findMethodOrThrow(slideToSeekBooleanMethod.definingClass)

                        val slideToSeekSyntheticIndex = slideToSeekConstructorMethod
                            .indexOfFirstInstructionReversedOrThrow {
                                opcode == Opcode.NEW_INSTANCE
                            }

                        val slideToSeekSyntheticClass = slideToSeekConstructorMethod
                            .getInstruction<ReferenceInstruction>(slideToSeekSyntheticIndex)
                            .reference
                            .toString()

                        val slideToSeekSyntheticMethod =
                            findMethodOrThrow(slideToSeekSyntheticClass) {
                                name == "run"
                            }

                        Pair(slideToSeekBooleanMethod, slideToSeekSyntheticMethod)
                    }
                }

            slideToSeekBooleanMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_OBJECT
                }
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL
                }

                hookSpeedOverlay(insertIndex, insertRegister, jumpIndex)
            }

            slideToSeekSyntheticMethod.apply {
                val speedOverlayFloatValueIndex = indexOfFirstInstructionOrThrow {
                    (this as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits()
                }
                val insertIndex =
                    indexOfFirstInstructionReversedOrThrow(speedOverlayFloatValueIndex) {
                        getReference<MethodReference>()?.name == "removeCallbacks"
                    } + 1
                val insertRegister =
                    getInstruction<FiveRegisterInstruction>(insertIndex - 1).registerC
                val jumpIndex =
                    indexOfFirstInstructionOrThrow(
                        speedOverlayFloatValueIndex,
                        Opcode.RETURN_VOID
                    ) + 1

                hookSpeedOverlay(insertIndex, insertRegister, jumpIndex)
            }

            // endregion

            // region patch for Custom speed overlay float value

            slideToSeekSyntheticMethod.apply {
                val speedOverlayFloatValueIndex = indexOfFirstInstructionOrThrow {
                    (this as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits()
                }
                val speedOverlayFloatValueRegister =
                    getInstruction<OneRegisterInstruction>(speedOverlayFloatValueIndex).registerA

                addInstructions(
                    speedOverlayFloatValueIndex + 1, """
                        invoke-static {v$speedOverlayFloatValueRegister}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue(F)F
                        move-result v$speedOverlayFloatValueRegister
                        """
                )
            }

            speedOverlayTextValueFingerprint.matchOrThrow().let {
                it.method.apply {
                    val targetIndex = it.patternMatch!!.startIndex
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue()D
                            move-result-wide v$targetRegister
                            """
                    )
                }
            }

            // endregion

        }
    }
}

private const val PLAYER_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlayerComponentsFilter;"
private const val SANITIZE_VIDEO_SUBTITLE_FILTER_CLASS_DESCRIPTOR =
    "$SPANS_PATH/SanitizeVideoSubtitleFilter;"

@Suppress("unused")
val playerComponentsPatch = bytecodePatch(
    PLAYER_COMPONENTS.title,
    PLAYER_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        controlsOverlayConfigPatch,
        inclusiveSpanPatch,
        lithoFilterPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        settingsPatch,
        speedOverlayPatch,
        suggestedVideoEndScreenPatch,
        videoInformationPatch,
    )

    execute {
        fun MutableMethod.getAllLiteralComponent(
            startIndex: Int,
            endIndex: Int
        ): String {
            var literalComponent = ""
            for (index in startIndex..endIndex) {
                val opcode = getInstruction(index).opcode
                if (opcode != Opcode.CONST_16 && opcode != Opcode.CONST_4)
                    continue

                val register = getInstruction<OneRegisterInstruction>(index).registerA
                val value = getInstruction<WideLiteralInstruction>(index).wideLiteral.toInt()

                val line = """
                const/16 v$register, $value
                
                """.trimIndent()

                literalComponent += line
            }

            return literalComponent
        }

        fun MutableMethod.getFirstLiteralComponent(
            startIndex: Int,
            endIndex: Int
        ): String {
            val constRegister =
                getInstruction<FiveRegisterInstruction>(endIndex).registerE

            for (index in endIndex downTo startIndex) {
                val instruction = getInstruction(index)
                if (instruction.opcode != Opcode.CONST_16 && instruction.opcode != Opcode.CONST_4)
                    continue

                if ((instruction as OneRegisterInstruction).registerA != constRegister)
                    continue

                val constValue = (instruction as WideLiteralInstruction).wideLiteral.toInt()

                return "const/16 v$constRegister, $constValue"
            }
            return ""
        }

        fun MutableMethod.hookFilmstripOverlay() {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay()Z
                    move-result v0
                    if-eqz v0, :shown
                    const/4 v0, 0x0
                    return v0
                    """, ExternalLabel("shown", getInstruction(0))
            )
        }

        // region patch for custom player overlay opacity

        youtubeControlsOverlayFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(scrimOverlay)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            if (!targetParameter.toString().endsWith("Landroid/widget/ImageView;"))
                throw PatchException("Method signature parameter did not match: $targetParameter")

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->changeOpacity(Landroid/widget/ImageView;)V"
            )
        }

        // endregion

        // region patch for disable auto player popup panels

        fun MutableMethod.hookInitVideoPanel(initVideoPanel: Int) =
            addInstructions(
                0, """
                    const/4 v0, $initVideoPanel
                    invoke-static {v0}, $PLAYER_CLASS_DESCRIPTOR->setInitVideoPanel(Z)V
                    """
            )

        arrayOf(
            lithoComponentOnClickListenerFingerprint,
            noticeOnClickListenerFingerprint,
            offlineActionsOnClickListenerFingerprint,
            startVideoInformerFingerprint,
        ).forEach { fingerprint ->
            fingerprint.methodOrThrow().apply {
                if (fingerprint == startVideoInformerFingerprint) {
                    hookInitVideoPanel(1)
                } else {
                    val syntheticIndex =
                        indexOfFirstInstruction(Opcode.NEW_INSTANCE)
                    if (syntheticIndex >= 0) {
                        val syntheticReference =
                            getInstruction<ReferenceInstruction>(syntheticIndex).reference.toString()

                        findMethodOrThrow(syntheticReference) {
                            name == "onClick"
                        }.hookInitVideoPanel(0)
                    } else {
                        println("WARNING: target Opcode not found in ${fingerprint.first}")
                    }
                }
            }
        }

        engagementPanelBuilderFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    move/from16 v0, p4
                    invoke-static {v0}, $PLAYER_CLASS_DESCRIPTOR->disableAutoPlayerPopupPanels(Z)Z
                    move-result v0
                    if-eqz v0, :shown
                    const/4 v0, 0x0
                    return-object v0
                    """, ExternalLabel("shown", getInstruction(0))
            )
        }

        // endregion

        // region patch for disable auto switch mix playlists

        hookVideoInformation("$PLAYER_CLASS_DESCRIPTOR->disableAutoSwitchMixPlaylists(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        // endregion

        // region patch for hide channel watermark

        watermarkFingerprint.matchOrThrow(watermarkParentFingerprint).let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->hideChannelWatermark(Z)Z
                        move-result v$register
                        """
                )
            }
        }

        // endregion

        // region patch for hide crowdfunding box

        crowdfundingBoxFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->hideCrowdfundingBox(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide double-tap overlay filter

        val smaliInstruction = """
            invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $PLAYER_CLASS_DESCRIPTOR->hideDoubleTapOverlayFilter(Landroid/view/View;)V
            """

        arrayOf(
            darkBackground,
            tapBloomView
        ).forEach { literal ->
            quickSeekOverlayFingerprint.injectLiteralInstructionViewCall(
                literal,
                smaliInstruction
            )
        }

        // endregion

        // region patch for hide end screen cards

        listOf(
            layoutCircleFingerprint,
            layoutIconFingerprint,
            layoutVideoFingerprint
        ).forEach { fingerprint ->
            fingerprint.matchOrThrow().let {
                it.method.apply {
                    val insertIndex = it.patternMatch!!.endIndex
                    val viewRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex + 1,
                        "invoke-static { v$viewRegister }, $PLAYER_CLASS_DESCRIPTOR->hideEndScreenCards(Landroid/view/View;)V"
                    )
                }
            }
        }

        // endregion

        // region patch for hide filmstrip overlay

        arrayOf(
            filmStripOverlayConfigFingerprint,
            filmStripOverlayInteractionFingerprint,
            filmStripOverlayPreviewFingerprint
        ).forEach { fingerprint ->
            fingerprint.methodOrThrow(filmStripOverlayParentFingerprint).hookFilmstripOverlay()
        }

        youtubeControlsOverlayFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(fadeDurationFast)
            val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA
            val insertIndex =
                indexOfFirstInstructionReversedOrThrow(constIndex, Opcode.INVOKE_VIRTUAL) + 1
            val jumpIndex = implementation!!.instructions.let { instruction ->
                insertIndex + instruction.subList(insertIndex, instruction.size - 1)
                    .indexOfFirst { instructions ->
                        instructions.opcode == Opcode.GOTO || instructions.opcode == Opcode.GOTO_16
                    }
            }

            val replaceInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
            val replaceReference =
                getInstruction<ReferenceInstruction>(insertIndex).reference

            addInstructionsWithLabels(
                insertIndex + 1, getAllLiteralComponent(insertIndex, jumpIndex - 1) + """
                    const v$constRegister, $fadeDurationFast
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay()Z
                    move-result v${replaceInstruction.registerA}
                    if-nez v${replaceInstruction.registerA}, :hidden
                    iget-object v${replaceInstruction.registerA}, v${replaceInstruction.registerB}, $replaceReference
                    """, ExternalLabel("hidden", getInstruction(jumpIndex))
            )
            removeInstruction(insertIndex)
        }

        // endregion

        // region patch for hide info cards

        infoCardsIncognitoFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.startIndex
                val targetRegister =
                    getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->hideInfoCard(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide seek message

        seekEduContainerFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekMessage()Z
                    move-result v0
                    if-eqz v0, :default
                    return-void
                    """, ExternalLabel("default", getInstruction(0))
            )
        }

        youtubeControlsOverlayFingerprint.methodOrThrow().apply {
            val insertIndex =
                indexOfFirstLiteralInstructionOrThrow(seekUndoEduOverlayStub)
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            val onClickListenerIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setOnClickListener"
            }
            val constComponent = getFirstLiteralComponent(insertIndex, onClickListenerIndex - 1)

            if (constComponent.isNotEmpty()) {
                addInstruction(
                    onClickListenerIndex + 2,
                    constComponent
                )
            }
            addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekUndoMessage()Z
                    move-result v$insertRegister
                    if-nez v$insertRegister, :default
                    """, ExternalLabel("default", getInstruction(onClickListenerIndex + 1))
            )
        }

        // endregion

        // region patch for hide suggested actions

        suggestedActionsFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->hideSuggestedActions(Landroid/view/View;)V"

                )
            }
        }

        // endregion

        // region patch for skip autoplay countdown

        // This patch works fine when the [SuggestedVideoEndScreenPatch] patch is included.
        touchAreaOnClickListenerFingerprint.mutableClassOrThrow().let {
            it.methods.find { method ->
                method.parameters == listOf("Landroid/view/View${'$'}OnClickListener;")
            }?.apply {
                val setOnClickListenerIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setOnClickListener"
                }
                val setOnClickListenerRegister =
                    getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                addInstruction(
                    setOnClickListenerIndex + 1,
                    "invoke-static {v$setOnClickListenerRegister}, $PLAYER_CLASS_DESCRIPTOR->skipAutoPlayCountdown(Landroid/view/View;)V"
                )
            } ?: throw PatchException("Failed to find setOnClickListener method")
        }

        // endregion

        // region patch for hide video zoom overlay

        videoZoomSnapIndicatorFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideZoomOverlay()Z
                    move-result v0
                    if-eqz v0, :shown
                    return-void
                    """, ExternalLabel("shown", getInstruction(0))
            )
        }

        // endregion

        addSpanFilter(SANITIZE_VIDEO_SUBTITLE_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(PLAYER_COMPONENTS_FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: PLAYER_COMPONENTS"
            ),
            PLAYER_COMPONENTS
        )

        // endregion

    }
}
