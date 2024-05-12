package app.revanced.patches.youtube.player.fullscreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.player.fullscreen.fingerprints.BroadcastReceiverFingerprint
import app.revanced.patches.youtube.player.fullscreen.fingerprints.ClientSettingEndpointFingerprint
import app.revanced.patches.youtube.player.fullscreen.fingerprints.EngagementPanelFingerprint
import app.revanced.patches.youtube.player.fullscreen.fingerprints.LandScapeModeConfigFingerprint
import app.revanced.patches.youtube.player.fullscreen.fingerprints.OrientationParentFingerprint
import app.revanced.patches.youtube.player.fullscreen.fingerprints.OrientationPrimaryFingerprint
import app.revanced.patches.youtube.player.fullscreen.fingerprints.OrientationSecondaryFingerprint
import app.revanced.patches.youtube.player.fullscreen.fingerprints.QuickActionsElementFingerprint
import app.revanced.patches.youtube.player.fullscreen.fingerprints.RelatedEndScreenResultsFingerprint
import app.revanced.patches.youtube.player.fullscreen.fingerprints.VideoPortraitParentFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AutoNavPreviewStub
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FullScreenEngagementPanel
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.QuickActionsElementContainer
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getWalkerMethod
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@Suppress("unused")
object FullscreenComponentsPatch : BaseBytecodePatch(
    name = "Fullscreen components",
    description = "Adds options to hide or change components related to fullscreen.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        BroadcastReceiverFingerprint,
        ClientSettingEndpointFingerprint,
        EngagementPanelFingerprint,
        LandScapeModeConfigFingerprint,
        LayoutConstructorFingerprint,
        OrientationParentFingerprint,
        QuickActionsElementFingerprint,
        RelatedEndScreenResultsFingerprint,
        VideoPortraitParentFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/QuickActionFilter;"

    override fun execute(context: BytecodeContext) {

        // region patch for disable engagement panel

        EngagementPanelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val literalIndex = getWideLiteralInstructionIndex(FullScreenEngagementPanel)
                val targetIndex = getTargetIndex(literalIndex, Opcode.CHECK_CAST)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, " +
                            "$PLAYER_CLASS_DESCRIPTOR->disableEngagementPanels(Landroidx/coordinatorlayout/widget/CoordinatorLayout;)V"
                )
            }
        }

        LayoutConstructorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndexWithMethodReferenceName("addView")
                val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

                replaceInstruction(
                    insertIndex,
                    "invoke-static { v${insertInstruction.registerC}, v${insertInstruction.registerD} }, " +
                            "$PLAYER_CLASS_DESCRIPTOR->showVideoTitleSection(Landroid/widget/FrameLayout;Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide autoplay preview

        LayoutConstructorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(AutoNavPreviewStub)
                val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA
                val jumpIndex = getTargetIndex(constIndex + 2, Opcode.INVOKE_VIRTUAL) + 1

                addInstructionsWithLabels(
                    constIndex, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideAutoPlayPreview()Z
                        move-result v$constRegister
                        if-nez v$constRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        }

        // endregion

        // region patch for hide related video overlay

        RelatedEndScreenResultsFingerprint.resultOrThrow().let {
            it.mutableClass.methods.find { method -> method.parameters == listOf("I", "Z", "I") }
                ?.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideRelatedVideoOverlay()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(0))
                    )
                } ?: throw PatchException("Could not find targetMethod")
        }

        // endregion

        // region patch for quick actions

        QuickActionsElementFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val containerCalls = implementation!!.instructions.withIndex()
                    .filter { instruction ->
                        (instruction.value as? WideLiteralInstruction)?.wideLiteral == QuickActionsElementContainer
                    }
                val constIndex = containerCalls.elementAt(containerCalls.size - 1).index

                val checkCastIndex = getTargetIndex(constIndex, Opcode.CHECK_CAST)
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->setQuickActionMargin(Landroid/widget/FrameLayout;)V"
                )

                addInstruction(
                    checkCastIndex,
                    "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hideQuickActions(Landroid/view/View;)V"
                )
            }
        }

        context.updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "QuickActions")

        // endregion

        // region patch for compact control overlay

        YouTubeControlsOverlayFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndexWithMethodReferenceName("setFocusableInTouchMode")
                val walkerIndex = getTargetIndex(targetIndex, Opcode.INVOKE_STATIC)

                val walkerMethod = getWalkerMethod(context, walkerIndex)
                walkerMethod.apply {
                    val insertIndex = implementation!!.instructions.size - 1
                    val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->enableCompactControlsOverlay(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for force fullscreen

        ClientSettingEndpointFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val getActivityIndex = getStringInstructionIndex("watch") + 2
                val getActivityReference =
                    getInstruction<ReferenceInstruction>(getActivityIndex).reference
                val classRegister =
                    getInstruction<TwoRegisterInstruction>(getActivityIndex).registerB

                val watchDescriptorMethodIndex =
                    getStringInstructionIndex("start_watch_minimized") - 1
                val watchDescriptorRegister =
                    getInstruction<FiveRegisterInstruction>(watchDescriptorMethodIndex).registerD

                addInstructions(
                    watchDescriptorMethodIndex, """
                        invoke-static {v$watchDescriptorRegister}, $PLAYER_CLASS_DESCRIPTOR->forceFullscreen(Z)Z
                        move-result v$watchDescriptorRegister
                        """
                )

                // hooks Activity.
                val insertIndex = getStringInstructionIndex("force_fullscreen")
                val freeRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        iget-object v$freeRegister, v$classRegister, $getActivityReference
                        check-cast v$freeRegister, Landroid/app/Activity;
                        invoke-static {v$freeRegister}, $PLAYER_CLASS_DESCRIPTOR->setWatchDescriptorActivity(Landroid/app/Activity;)V
                        """
                )
            }
        }

        VideoPortraitParentFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val stringIndex =
                    getStringInstructionIndex("Acquiring NetLatencyActionLogger failed. taskId=")
                val invokeIndex = getTargetIndex(stringIndex, Opcode.INVOKE_INTERFACE)
                val targetIndex = getTargetIndex(invokeIndex, Opcode.CHECK_CAST)
                val targetClass = context
                    .findClass(getInstruction<ReferenceInstruction>(targetIndex).reference.toString())!!
                    .mutableClass

                // add an instruction to check the vertical video
                targetClass.methods.find { method -> method.parameters == listOf("I", "I", "Z") }
                    ?.apply {
                        addInstruction(
                            1,
                            "invoke-static {p1, p2}, $PLAYER_CLASS_DESCRIPTOR->setVideoPortrait(II)V"
                        )
                    } ?: throw PatchException("Could not find targetMethod")
            }
        }

        // endregion

        // region patch for disable landscape mode

        OrientationParentFingerprint.resultOrThrow().classDef.let { classDef ->
            arrayOf(
                OrientationPrimaryFingerprint,
                OrientationSecondaryFingerprint
            ).forEach { fingerprint ->
                fingerprint.resolve(context, classDef)

                fingerprint.resultOrThrow().let {
                    it.mutableMethod.apply {
                        val index = it.scanResult.patternScanResult!!.endIndex
                        val register = getInstruction<OneRegisterInstruction>(index).registerA

                        addInstructions(
                            index + 1, """
                                    invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->disableLandScapeMode(Z)Z
                                    move-result v$register
                                    """
                        )
                    }
                }
            }
        }

        // endregion

        // region patch for keep landscape mode

        if (SettingsPatch.upward1842) {
            LandScapeModeConfigFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = implementation!!.instructions.size - 1
                    val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->keepFullscreen(Z)Z
                            move-result v$insertRegister
                            """
                    )
                }

                BroadcastReceiverFingerprint.resultOrThrow().let { result ->
                    result.mutableMethod.apply {
                        val stringIndex = getStringInstructionIndex("android.intent.action.SCREEN_ON")
                        val insertIndex = getTargetIndex(stringIndex, Opcode.IF_EQZ) + 1

                        addInstruction(
                            insertIndex,
                            "invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->setScreenOn()V"
                        )
                    }
                }

                SettingsPatch.addPreference(
                    arrayOf(
                        "SETTINGS: KEEP_LANDSCAPE_MODE"
                    )
                )
            }
        }

        // endregion

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)


        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: FULLSCREEN_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
