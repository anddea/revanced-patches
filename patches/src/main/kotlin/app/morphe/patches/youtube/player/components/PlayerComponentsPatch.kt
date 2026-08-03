/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.components

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.misc.fix.proto.immutableMethodRef
import app.morphe.patches.shared.misc.fix.proto.mutableCopyMethodRef
import app.morphe.patches.shared.spans.addSpanFilter
import app.morphe.patches.shared.spans.inclusiveSpanPatch
import app.morphe.patches.shared.startVideoInformerFingerprint
import app.morphe.patches.shared.textcomponent.hookSpannableString
import app.morphe.patches.shared.textcomponent.textComponentPatch
import app.morphe.patches.youtube.shared.WatchNextResponseParserFingerprint
import app.morphe.patches.youtube.utils.bottomsheet.bottomSheetHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.dismiss.dismissPlayerHookPatch
import app.morphe.patches.youtube.utils.engagement.engagementPanelBuilderMethod
import app.morphe.patches.youtube.utils.engagement.engagementPanelFreeRegister
import app.morphe.patches.youtube.utils.engagement.engagementPanelHookPatch
import app.morphe.patches.youtube.utils.engagement.engagementPanelIdIndex
import app.morphe.patches.youtube.utils.engagement.engagementPanelIdRegister
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.extension.Constants.SPANS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.fix.endscreensuggestedvideo.endScreenSuggestedVideoPatch
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.PLAYER_COMPONENTS
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_18_39_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_18_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_43_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_02_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_03_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_05_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_09_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_12_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_14_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_15_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_16_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_18_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_19_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.darkBackground
import app.morphe.patches.youtube.utils.resourceid.eduOverlayStub
import app.morphe.patches.youtube.utils.resourceid.fadeDurationFast
import app.morphe.patches.youtube.utils.resourceid.scrimOverlay
import app.morphe.patches.youtube.utils.resourceid.seekUndoEduOverlayStub
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.resourceid.tapBloomView
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.youtubeControlsOverlayFingerprint
import app.morphe.patches.youtube.video.information.hookVideoInformation
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.REGISTER_TEMPLATE_REPLACEMENT
import app.morphe.util.Utils.printWarn
import app.morphe.util.findMethodOrThrow
import app.morphe.util.findMutableMethodOf
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.injectLiteralInstructionViewCall
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.fingerprint.mutableClassOrThrow
import app.morphe.util.getReference
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private val speedOverlayPatch = bytecodePatch(
    description = "speedOverlayPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
        textComponentPatch,
        versionCheckPatch,
    )

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

        fun MutableMethod.hookRelativeSpeedValue(startIndex: Int) {
            val relativeIndex = indexOfFirstInstructionOrThrow(startIndex, Opcode.CMPL_FLOAT)
            val relativeRegister = getInstruction<ThreeRegisterInstruction>(relativeIndex).registerB

            addInstructions(
                relativeIndex, """
                    invoke-static {v$relativeRegister}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayRelativeValue(F)F
                    move-result v$relativeRegister
                    """
            )
        }

        if (!is_19_18_or_greater) {
            // Used on YouTube 18.29.38 ~ YouTube 19.17.41

            // region patch for Disable speed overlay (Enable slide to seek)

            mapOf(
                restoreSlideToSeekBehaviorFingerprint to RESTORE_SLIDE_TO_SEEK_FEATURE_FLAG,
                speedOverlayFingerprint to SPEED_OVERLAY_FEATURE_FLAG
            ).forEach { (fingerprint, literal) ->
                fingerprint.injectLiteralInstructionBooleanCall(
                    literal,
                    "$PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay(Z)Z"
                )
            }

            // endregion

            // region patch for Custom speed overlay float value

            val speedFieldReference = with(speedOverlayFloatValueFingerprint.methodOrThrow()) {
                val literalIndex =
                    indexOfFirstLiteralInstructionOrThrow(SPEED_OVERLAY_LEGACY_FEATURE_FLAG)
                val floatIndex =
                    indexOfFirstInstructionOrThrow(literalIndex, Opcode.DOUBLE_TO_FLOAT)
                val floatRegister = getInstruction<TwoRegisterInstruction>(floatIndex).registerA

                addInstructions(
                    floatIndex + 1, """
                        invoke-static {v$floatRegister}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue(F)F
                        move-result v$floatRegister
                        """
                )

                val speedFieldIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                    opcode == Opcode.IPUT &&
                            getReference<FieldReference>()?.type == "F"
                }

                getInstruction<ReferenceInstruction>(speedFieldIndex).reference.toString()
            }

            fun indexOfFirstSpeedFieldInstruction(method: Method) =
                method.indexOfFirstInstruction {
                    opcode == Opcode.IGET &&
                            getReference<FieldReference>()?.toString() == speedFieldReference
                }

            val isSyntheticMethod: Method.() -> Boolean = {
                name == "run" &&
                        accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL &&
                        parameterTypes.isEmpty() &&
                        indexOfFirstSpeedFieldInstruction(this) >= 0 &&
                        indexOfFirstInstruction(Opcode.CMPL_FLOAT) >= 0
            }

            classDefForEach { classDef ->
                classDef.methods.forEach { method ->
                    if (method.isSyntheticMethod()) {
                        mutableClassDefBy(classDef)
                            .findMutableMethodOf(method)
                            .apply {
                                val speedFieldIndex = indexOfFirstSpeedFieldInstruction(this)
                                hookRelativeSpeedValue(speedFieldIndex)
                            }
                    }
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
                        val startIndex = it.instructionMatches.first().index
                        val endIndex = it.instructionMatches.last().index
                        val jumpIndex = endIndex + 1
                        val insertIndex = endIndex - 1
                        val insertRegister =
                            getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                        hookSpeedOverlay(insertIndex, insertRegister, jumpIndex)

                        val slideToSeekBooleanMethod =
                            getWalkerMethod(startIndex + 1)

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

                hookRelativeSpeedValue(speedOverlayFloatValueIndex)
            }

            // Removed in YouTube 20.03+
            if (is_20_03_or_greater) {
                hookSpannableString(
                    PLAYER_CLASS_DESCRIPTOR,
                    "onCharSequenceLoaded"
                )
            } else {
                speedOverlayTextValueFingerprint.methodOrThrow().apply {
                    val targetIndex =
                        indexOfFirstInstructionOrThrow(Opcode.CONST_WIDE_HIGH16)
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
private const val RELATED_VIDEO_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/RelatedVideoPatch;"

@Suppress("unused")
val playerComponentsPatch = bytecodePatch(
    PLAYER_COMPONENTS.title,
    PLAYER_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        bottomSheetHookPatch,
        endScreenSuggestedVideoPatch,
        engagementPanelHookPatch,
        fixProtoLibraryPatch,
        dismissPlayerHookPatch,
        inclusiveSpanPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        speedOverlayPatch,
        videoInformationPatch,
        versionCheckPatch,
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

        if (is_20_05_or_greater) {
            engagementPanelBuilderMethod.addInstructionsWithLabels(
                engagementPanelIdIndex, """
                    move/from16 v$engagementPanelFreeRegister, p4
                    invoke-static {v$engagementPanelFreeRegister, v$engagementPanelIdRegister}, $PLAYER_CLASS_DESCRIPTOR->disableAutoPlayerPopupPanels(ZLjava/lang/String;)Z
                    move-result v$engagementPanelFreeRegister
                    if-eqz v$engagementPanelFreeRegister, :shown
                    const/4 v$engagementPanelFreeRegister, 0x0
                    return-object v$engagementPanelFreeRegister
                    :shown
                    nop
                    """
            )
            hookVideoInformation("$PLAYER_CLASS_DESCRIPTOR->disableAutoPlayerPopupPanels(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        } else {
            arrayOf(
                lithoComponentOnClickListenerFingerprint,
                offlineActionsOnClickListenerFingerprint,
            ).forEach { fingerprint ->
                fingerprint.methodOrThrow().apply {
                    val syntheticIndex =
                        indexOfFirstInstruction(Opcode.NEW_INSTANCE)
                    if (syntheticIndex >= 0) {
                        val syntheticReference =
                            getInstruction<ReferenceInstruction>(syntheticIndex).reference.toString()

                        findMethodOrThrow(syntheticReference) {
                            name == "onClick"
                        }.hookInitVideoPanel(0)
                    } else {
                        printWarn("target Opcode not found in ${fingerprint.first}")
                    }
                }
            }

            findMethodOrThrow(
                engagementPanelPlaylistSyntheticFingerprint.methodOrThrow().definingClass
            ) {
                name == "onClick"
            }.hookInitVideoPanel(0)

            startVideoInformerFingerprint.methodOrThrow().hookInitVideoPanel(1)

            engagementPanelBuilderMethod.addInstructionsWithLabels(
                0, """
                    move/from16 v0, p4
                    invoke-static {v0}, $PLAYER_CLASS_DESCRIPTOR->disableAutoPlayerPopupPanels(Z)Z
                    move-result v0
                    if-eqz v0, :shown
                    const/4 v0, 0x0
                    return-object v0
                    :shown
                    nop
                    """
            )
        }

        // endregion

        // region patch for disable auto switch mix playlists

        hookVideoInformation("$PLAYER_CLASS_DESCRIPTOR->disableAutoSwitchMixPlaylists(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        // endregion

        // region patch for disable double tap chapters

        mapOf(
            doubleTapInfoConstructorFingerprint to "p3",
            doubleTapInfoGetSeekSourceFingerprint to "p1",
        ).forEach { (fingerprint, parameter) ->
            fingerprint
                .methodOrThrow(doubleTapInfoFloatFingerprint)
                .addInstructions(
                    0, """
                        invoke-static { $parameter }, $PLAYER_CLASS_DESCRIPTOR->disableDoubleTapChapters(Z)Z
                        move-result $parameter
                        """
                )
        }

        // endregion

        // region patch for hide channel watermark

        watermarkFingerprint.matchOrThrow(watermarkParentFingerprint).let {
            it.method.apply {
                val insertIndex = it.instructionMatches.last().index
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
                val insertIndex = it.instructionMatches.last().index
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
            endScreenElementLayoutCircleFingerprint,
            endScreenElementLayoutIconFingerprint,
            endScreenElementLayoutVideoFingerprint
        ).forEach { fingerprint ->
            fingerprint.matchOrThrow().let {
                it.method.apply {
                    val insertIndex = it.instructionMatches.last().index
                    val viewRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex + 1,
                        "invoke-static { v$viewRegister }, $PLAYER_CLASS_DESCRIPTOR->hideEndScreenCards(Landroid/view/View;)V"
                    )
                }
            }
        }

        if (is_19_43_or_greater) {
            endScreenPlayerResponseModelFingerprint
                .methodOrThrow()
                .addInstructionsWithLabels(
                    0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideEndScreenCards()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    :show
                    nop
                    """
                )
        }

        // endregion

        // region patch for hide filmstrip overlay

        fun MutableMethod.hookFilmstripOverlay(
            index: Int = 0,
            register: Int = 0
        ) {
            val stringInstructions = when (returnType) {
                "Z" -> """
                    const/4 v$register, 0x0
                    return v$register
                    """

                "V" -> """
                    return-void
                    """

                else -> throw Exception("This case should never happen.")
            }

            addInstructionsWithLabels(
                index, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay()Z
                    move-result v$register
                    if-eqz v$register, :shown
                    """ + stringInstructions + """
                        :shown
                        nop
                    """
            )
        }

        val filmStripOverlayFingerprints = mutableListOf(
            filmStripOverlayInteractionFingerprint,
            filmStripOverlayPreviewFingerprint
        )

        if (is_20_12_or_greater) {
            filmStripOverlayMotionEventPrimaryFingerprint.matchOrThrow(
                filmStripOverlayStartParentFingerprint
            ).let {
                it.method.apply {
                    val index = it.instructionMatches.first().index
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    hookFilmstripOverlay(index, register)
                }
            }

            filmStripOverlayMotionEventSecondaryFingerprint.matchOrThrow(
                filmStripOverlayStartParentFingerprint
            ).let {
                it.method.apply {
                    val index = it.instructionMatches.first().index + 2
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructions(
                        index, """
                            invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        } else {
            filmStripOverlayFingerprints += filmStripOverlayConfigFingerprint
        }

        filmStripOverlayFingerprints.forEach { fingerprint ->
            fingerprint.methodOrThrow(filmStripOverlayEnterParentFingerprint).hookFilmstripOverlay()
        }

        // Removed in YouTube 20.03+
        if (!is_20_03_or_greater) {
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
        } else if (is_20_05_or_greater) {
            // This is a new filmstrip overlay added to YouTube 20.05+
            // Disabling this flag is not related to the operation of the patch.
            filmStripOverlayConfigV2Fingerprint.injectLiteralInstructionBooleanCall(
                FILM_STRIP_OVERLAY_V2_FEATURE_FLAG,
                "0x0"
            )
        }

        // endregion

        // region patch for hide info cards

        infoCardsIncognitoFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.instructionMatches.first().index
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

        // region patch for hide related videos

        val continuationsField = with(WatchNextResponseParserFingerprint) {
            clearMatch()
            instructionMatches[2].instruction.getReference<FieldReference>()!!
        }
        val resultsClass = continuationsField.definingClass
        val helperMethodParameter = listOf(
            ImmutableMethodParameter(resultsClass, null, null),
        )

        val emptyProtobufListMethod = Fingerprint(
            definingClass = resultsClass,
            name = "<init>",
            returnType = "V",
            parameters = emptyList(),
            filters = listOf(
                methodCall(
                    opcode = Opcode.INVOKE_STATIC,
                    name = "emptyProtobufList",
                ),
            ),
        ).instructionMatches.last().instruction.getReference<MethodReference>()!!

        val sectionIdentifierField = RelatedItemSectionFingerprint
            .instructionMatches[1].instruction.getReference<FieldReference>()!!
        val watchNextResponseModelClass = WatchNextResponseModelClassResolverFingerprint
            .instructionMatches.last().instruction.getReference<TypeReference>()!!.type

        Fingerprint(
            definingClass = watchNextResponseModelClass,
            accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
            returnType = "V",
            parameters = listOf(resultsClass),
            filters = listOf(
                fieldAccess(
                    opcode = Opcode.IGET_OBJECT,
                    definingClass = resultsClass,
                    type = emptyProtobufListMethod.returnType,
                ),
                methodCall(
                    opcode = Opcode.INVOKE_INTERFACE,
                    name = "iterator",
                    location = MatchAfterImmediately(),
                ),
                fieldAccess(
                    opcode = Opcode.IGET_OBJECT,
                    type = sectionIdentifierField.definingClass,
                ),
            ),
        ).let {
            val contentsField =
                it.instructionMatches.first().instruction.getReference<FieldReference>()!!
            val itemSectionRendererField =
                it.instructionMatches.last().instruction.getReference<FieldReference>()!!
            val itemSectionRendererDefaultInstance =
                "${itemSectionRendererField.type}->a:${itemSectionRendererField.type}"

            val shelfRendererField = Fingerprint(
                returnType = "Ljava/util/List;",
                parameters = listOf("Ljava/lang/Object;"),
                filters = listOf(
                    string("hint=%s,(%s=%s,cheatsheet=%b,key1=%s,w=%d,h=%d)"),
                    fieldAccess(
                        opcode = Opcode.IGET_OBJECT,
                        definingClass = itemSectionRendererField.definingClass,
                    ),
                    fieldAccess(
                        opcode = Opcode.IGET_OBJECT,
                        reference = itemSectionRendererField,
                    ),
                ),
            ).instructionMatches[1].instruction.getReference<FieldReference>()!!
            val shelfRendererDefaultInstance =
                "${shelfRendererField.type}->a:${shelfRendererField.type}"

            it.method.apply {
                val helperMethod = ImmutableMethod(
                    definingClass,
                    "patch_hideRelatedVideos",
                    helperMethodParameter,
                    "V",
                    AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                    annotations,
                    null,
                    MutableMethodImplementation(6),
                ).toMutable().apply {
                    addInstructionsWithLabels(
                        0,
                        """
                            invoke-static {}, $RELATED_VIDEO_CLASS_DESCRIPTOR->hideRelatedVideos()Z
                            move-result v0

                            if-eqz v0, :ignore
                            iget-object v0, p1, $contentsField
                            invoke-interface {v0}, ${immutableMethodRef.get()}
                            move-result v1

                            # Check if ProtoList is immutable or not.
                            if-nez v1, :ignore

                            # If mutable, copy the ProtoList.
                            invoke-static {v0}, ${mutableCopyMethodRef.get()}
                            move-result-object v0

                            invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                            move-result-object v1

                            :loop
                            invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z
                            move-result v2

                            if-eqz v2, :exit
                            invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                            move-result-object v2
                            check-cast v2, ${itemSectionRendererField.definingClass}

                            # Replace related ItemSectionRenderer content while preserving comments.
                            invoke-static {v2}, $RELATED_VIDEO_CLASS_DESCRIPTOR->isRelatedItems(Lcom/google/protobuf/MessageLite;)Z
                            move-result v3
                            if-eqz v3, :is_not_related_item

                            sget-object v3, $itemSectionRendererDefaultInstance
                            iput-object v3, v2, $itemSectionRendererField
                            goto :loop

                            :is_not_related_item

                            # ShelfRenderer is used for related videos on tablets.
                            invoke-static {v2}, $RELATED_VIDEO_CLASS_DESCRIPTOR->isShelfRenderer(Lcom/google/protobuf/MessageLite;)Z
                            move-result v3
                            if-eqz v3, :loop

                            sget-object v3, $shelfRendererDefaultInstance
                            iput-object v3, v2, $shelfRendererField
                            goto :loop

                            :exit
                            invoke-static {}, $RELATED_VIDEO_CLASS_DESCRIPTOR->isFiltered()Z
                            move-result v2
                            if-eqz v2, :ignore

                            iput-object v0, p1, $contentsField

                            # Empty continuations prevent redundant related-video requests.
                            invoke-static {}, $emptyProtobufListMethod
                            move-result-object v0
                            iput-object v0, p1, $continuationsField

                            :ignore
                            return-void
                        """,
                    )
                }

                it.classDef.methods.add(helperMethod)
                addInstruction(0, "invoke-direct {p0, p1}, $helperMethod")
            }
        }

        // endregion

        // region patch for hide seek message (Removed in YouTube 2003+)

        if (!is_20_03_or_greater) {
            seekEduContainerFingerprint.methodOrThrow().apply {
                val (register, condition, insertIndex) = when {
                is_20_18_or_greater && !is_20_19_or_greater -> {
                    Triple("v2", "if-nez", 0)
                }

                !is_20_14_or_greater || (is_20_15_or_greater && !is_20_16_or_greater) -> {
                    Triple("v0", "if-eqz", 0)
                }

                else -> {
                    Triple("v0", "if-eqz", 1)
                }
            }
            val labelInstruction = getInstruction(insertIndex)
            addInstructionsWithLabels(
                insertIndex,
                """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekMessage()Z
                    move-result $register
                    $condition $register, :default
                    return-void
                """,
                ExternalLabel("default", labelInstruction)
                )
            }

            if (!is_20_02_or_greater) {
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
            }
        }

        if (is_18_39_or_greater && !is_20_03_or_greater) {
            playerEduOverlayFeatureFlagFingerprint.methodOrThrow().apply {
                val targetIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex,
                    "const/4 v$targetRegister, 0x0"
                )
            }
        } else if (is_20_03_or_greater && !is_20_09_or_greater) {
            youtubeControlsOverlayFingerprint.methodOrThrow().apply {
                val constIndex = indexOfFirstLiteralInstructionOrThrow(eduOverlayStub)
                val targetIndex = indexOfFirstInstructionOrThrow(constIndex) {
                    opcode == Opcode.CHECK_CAST &&
                            getReference<TypeReference>()?.type == "Landroid/view/ViewStub;"
                }
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static { v$targetRegister }, $PLAYER_CLASS_DESCRIPTOR->hideSeekMessage(Landroid/view/ViewStub;)Landroid/view/ViewStub;
                        move-result-object v$targetRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide suggested actions

        suggestedActionsFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.instructionMatches.last().index
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->hideSuggestedActions(Landroid/view/View;)V"

                )
            }
        }

        // endregion

        // region patch for skip autoplay countdown

        // This patch works fine when the [EndScreenSuggestedVideoPatch] patch is included.
        touchAreaOnClickListenerFingerprint.mutableClassOrThrow().let {
            it.methods.find { method ->
                method.parameters == listOf($$"Landroid/view/View$OnClickListener;")
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
                "SETTINGS: PLAYER_COMPONENTS",
            ),
            PLAYER_COMPONENTS
        )
        // endregion

    }
}
