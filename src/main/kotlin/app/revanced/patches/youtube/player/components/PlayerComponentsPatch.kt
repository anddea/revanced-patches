package app.revanced.patches.youtube.player.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.player.components.fingerprints.CrowdfundingBoxFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.EngagementPanelControllerFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.FilmStripOverlayConfigFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.FilmStripOverlayInteractionFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.FilmStripOverlayParentFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.FilmStripOverlayPreviewFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.InfoCardsIncognitoFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.LayoutCircleFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.LayoutIconFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.LayoutVideoFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.RestoreSlideToSeekBehaviorFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.SeekEduContainerFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.SpeedOverlayFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.SpeedOverlayValueFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.SuggestedActionsFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.TouchAreaOnClickListenerFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.WatermarkFingerprint
import app.revanced.patches.youtube.player.components.fingerprints.WatermarkParentFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.controlsoverlay.ControlsOverlayConfigPatch
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.fix.suggestedvideoendscreen.SuggestedVideoEndScreenPatch
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FadeDurationFast
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ScrimOverlay
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.SeekUndoEduOverlayStub
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Suppress("unused")
object PlayerComponentsPatch : BaseBytecodePatch(
    name = "Player components",
    description = "Adds options to hide or change components related to player.",
    dependencies = setOf(
        ControlsOverlayConfigPatch::class,
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        SuggestedVideoEndScreenPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        CrowdfundingBoxFingerprint,
        EngagementPanelControllerFingerprint,
        FilmStripOverlayParentFingerprint,
        InfoCardsIncognitoFingerprint,
        LayoutCircleFingerprint,
        LayoutIconFingerprint,
        LayoutVideoFingerprint,
        RestoreSlideToSeekBehaviorFingerprint,
        SeekEduContainerFingerprint,
        SpeedOverlayFingerprint,
        SpeedOverlayValueFingerprint,
        SuggestedActionsFingerprint,
        TouchAreaOnClickListenerFingerprint,
        WatermarkParentFingerprint,
        YouTubeControlsOverlayFingerprint,
    )
) {
    private const val PLAYER_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlayerComponentsFilter;"

    override fun execute(context: BytecodeContext) {

        // region patch for custom player overlay opacity

        YouTubeControlsOverlayFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(ScrimOverlay)
                val targetIndex = getTargetIndex(constIndex, Opcode.CHECK_CAST)
                val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                if (!targetParameter.toString().endsWith("Landroid/widget/ImageView;"))
                    throw PatchException("Method signature parameter did not match: $targetParameter")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->changePlayerOpacity(Landroid/widget/ImageView;)V"
                )
            }
        }

        // endregion

        // region patch for disable auto player popup panels

        EngagementPanelControllerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableAutoPlayerPopupPanels()Z
                        move-result v0
                        if-eqz v0, :shown
                        # The type of the fourth parameter is boolean.
                        if-eqz p4, :shown
                        const/4 v0, 0x0
                        return-object v0
                        """, ExternalLabel("shown", getInstruction(0))
                )
            }
        }

        // endregion

        // region patch for disable speed overlay and speed overlay value

        mapOf(
            RestoreSlideToSeekBehaviorFingerprint to 45411329,
            SpeedOverlayFingerprint to 45411330
        ).forEach { (fingerprint, literal) ->
            fingerprint.literalInstructionBooleanHook(
                literal,
                "$PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay(Z)Z"
            )
        }

        SpeedOverlayValueFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val index = it.scanResult.patternScanResult!!.startIndex
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

        // region patch for hide channel watermark

        WatermarkFingerprint.resolve(
            context,
            WatermarkParentFingerprint.resultOrThrow().classDef
        )
        WatermarkFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
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

        CrowdfundingBoxFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->hideCrowdfundingBox(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide end screen cards

        listOf(
            LayoutCircleFingerprint,
            LayoutIconFingerprint,
            LayoutVideoFingerprint
        ).forEach{ fingerprint ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
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

        FilmStripOverlayParentFingerprint.resultOrThrow().classDef.let { classDef ->
            arrayOf(
                FilmStripOverlayConfigFingerprint,
                FilmStripOverlayInteractionFingerprint,
                FilmStripOverlayPreviewFingerprint
            ).forEach { fingerprint ->
                fingerprint.resolve(context, classDef)
                fingerprint.resultOrThrow().mutableMethod.hook()
            }
        }

        YouTubeControlsOverlayFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(FadeDurationFast)
                val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA
                val insertIndex = getTargetIndexReversed(constIndex, Opcode.INVOKE_VIRTUAL) + 1
                val jumpIndex = implementation!!.instructions.let { instruction ->
                    insertIndex + instruction.subList(insertIndex, instruction.size - 1).indexOfFirst { instructions ->
                        instructions.opcode == Opcode.GOTO || instructions.opcode == Opcode.GOTO_16
                    }
                }

                val replaceInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                val replaceReference =
                    getInstruction<ReferenceInstruction>(insertIndex).reference

                addLiteralValues(insertIndex, jumpIndex - 1)

                addInstructionsWithLabels(
                    insertIndex + 1, """
                        const v$constRegister, $FadeDurationFast
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay()Z
                        move-result v${replaceInstruction.registerA}
                        if-nez v${replaceInstruction.registerA}, :hidden
                        iget-object v${replaceInstruction.registerA}, v${replaceInstruction.registerB}, $replaceReference
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
                removeInstruction(insertIndex)
            }
        }

        // endregion

        // region patch for hide info cards

        InfoCardsIncognitoFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
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

        SeekEduContainerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekMessage()Z
                        move-result v0
                        if-eqz v0, :default
                        return-void
                        """, ExternalLabel("default", getInstruction(0))
                )
            }
        }

        YouTubeControlsOverlayFingerprint.resultOrThrow().let { result ->
            result.mutableMethod.apply {
                val insertIndex = getWideLiteralInstructionIndex(SeekUndoEduOverlayStub)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                val onClickListenerIndex = getTargetIndexWithMethodReferenceName(insertIndex, "setOnClickListener")
                val constComponent = getConstComponent(insertIndex, onClickListenerIndex - 1)

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

        // endregion

        // region patch for hide suggested actions

        SuggestedActionsFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
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
        TouchAreaOnClickListenerFingerprint.resultOrThrow().let {
            it.mutableClass.methods.find { method ->
                method.parameters == listOf("Landroid/view/View${'$'}OnClickListener;")
            }?.apply {
                val setOnClickListenerIndex = getTargetIndexWithMethodReferenceName("setOnClickListener")
                val setOnClickListenerRegister = getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                addInstruction(
                    setOnClickListenerIndex + 1,
                    "invoke-static {v$setOnClickListenerRegister}, $PLAYER_CLASS_DESCRIPTOR->skipAutoPlayCountdown(Landroid/view/View;)V"
                )
            } ?: throw PatchException("Failed to find setOnClickListener method")
        }

        // endregion

        LithoFilterPatch.addFilter(PLAYER_COMPONENTS_FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: PLAYER_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }

    private var literalComponent: String = ""

    private fun MutableMethod.addLiteralValues(
        startIndex: Int,
        endIndex: Int
    ) {
        for (index in startIndex..endIndex) {
            val opcode = getInstruction(index).opcode
            if (opcode != Opcode.CONST_16 && opcode != Opcode.CONST_4 && opcode != Opcode.CONST)
                continue

            val register = getInstruction<OneRegisterInstruction>(index).registerA
            val value = getInstruction<WideLiteralInstruction>(index).wideLiteral.toInt()

            val line =
                when (opcode) {
                    Opcode.CONST_16 -> """
                            const/16 v$register, $value
                            
                            """.trimIndent()

                    Opcode.CONST_4 -> """
                            const/4 v$register, $value
                            
                            """.trimIndent()

                    Opcode.CONST -> """
                            const v$register, $value
                            
                            """.trimIndent()

                    else -> ""
                }

            literalComponent += line
        }
    }

    private fun MutableMethod.hook() {
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

    private fun MutableMethod.getConstComponent(
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
}
