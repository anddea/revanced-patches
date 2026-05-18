package app.morphe.patches.youtube.player.buttons

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.fix.bottomui.cfBottomUIPatch
import app.morphe.patches.youtube.utils.inflateControlsGroupLayoutStubFingerprint
import app.morphe.patches.youtube.utils.layoutConstructorFingerprint
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_PLAYER_BUTTONS
import app.morphe.patches.youtube.utils.playservice.is_18_31_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_34_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_28_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.*
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findFreeRegister
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import app.morphe.util.insertLiteralOverride
import app.morphe.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val HAS_NEXT = 5
private const val HAS_PREVIOUS = 6
private const val PLAYER_CAST_BUTTON_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/PlayerCastButtonPatch;"

@Suppress("unused")
val playerButtonsPatch = bytecodePatch(
    HIDE_PLAYER_BUTTONS.title,
    HIDE_PLAYER_BUTTONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        cfBottomUIPatch,
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
    )

    execute {

        // region patch for hide autoplay button

        layoutConstructorFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(autoNavToggle)
            val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA
            val jumpIndex =
                indexOfFirstInstructionOrThrow(constIndex + 2, Opcode.INVOKE_VIRTUAL) + 1

            addInstructionsWithLabels(
                constIndex, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideAutoPlayButton()Z
                    move-result v$constRegister
                    if-nez v$constRegister, :hidden
                    """, ExternalLabel("hidden", getInstruction(jumpIndex))
            )
        }

        // endregion

        // region patch for hide captions button

        if (is_18_31_or_greater) {
            LithoSubtitleButtonConfigFingerprint.method.insertLiteralOverride(
                LITHO_SUBTITLE_BUTTON_FEATURE_FLAG,
                "$PLAYER_CLASS_DESCRIPTOR->hideCaptionsButton(Z)Z"
            )
        }

        YoutubeControlsOverlaySubtitleButtonFingerprint.method.apply {
            val insertIndex = implementation!!.instructions.lastIndex
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex,
                "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hideCaptionsButton(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for hide cast button

        MediaRouteButtonFingerprint.method.addInstructions(
            0,
            """
                invoke-static { p1 }, $PLAYER_CAST_BUTTON_CLASS_DESCRIPTOR->hideCastButton(I)I
                move-result p1
            """
        )

        if (is_20_28_or_greater) {
            arrayOf(
                CastButtonPlayerFeatureFlagFingerprint,
                CastButtonActionFeatureFlagFingerprint,
            ).forEach { fingerprint ->
                fingerprint.method.insertLiteralOverride(
                    fingerprint.instructionMatches.first().index,
                    "$PLAYER_CAST_BUTTON_CLASS_DESCRIPTOR->getCastButtonOverride(Z)Z",
                )
            }
        }

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "PlayerButtons")

        // endregion

        // region patch for hide collapse button

        TitleAnchorFingerprint.method.apply {
            val titleAnchorConstIndex = indexOfFirstLiteralInstructionOrThrow(titleAnchor)
            val titleAnchorIndex =
                indexOfFirstInstructionOrThrow(titleAnchorConstIndex, Opcode.MOVE_RESULT_OBJECT)
            val titleAnchorRegister =
                getInstruction<OneRegisterInstruction>(titleAnchorIndex).registerA

            addInstruction(
                titleAnchorIndex + 1,
                "invoke-static {v$titleAnchorRegister}, $PLAYER_CLASS_DESCRIPTOR->setTitleAnchorStartMargin(Landroid/view/View;)V"
            )

            val playerCollapseButtonConstIndex =
                indexOfFirstLiteralInstructionOrThrow(playerCollapseButton)
            val playerCollapseButtonIndex =
                indexOfFirstInstructionOrThrow(playerCollapseButtonConstIndex, Opcode.CHECK_CAST)
            val playerCollapseButtonRegister =
                getInstruction<OneRegisterInstruction>(playerCollapseButtonIndex).registerA

            addInstruction(
                playerCollapseButtonIndex + 1,
                "invoke-static {v$playerCollapseButtonRegister}, $PLAYER_CLASS_DESCRIPTOR->hideCollapseButton(Landroid/widget/ImageView;)V"
            )
        }

        // endregion

        // region patch for hide fullscreen button

        FullScreenButtonFingerprint.method.apply {
            val buttonCalls = implementation!!.instructions.withIndex()
                .filter { instruction ->
                    (instruction.value as? WideLiteralInstruction)?.wideLiteral == fullScreenButton
                }
            val constIndex = buttonCalls.elementAt(buttonCalls.size - 1).index
            val castIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val insertIndex = castIndex + 1
            val insertRegister = getInstruction<OneRegisterInstruction>(castIndex).registerA

            addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hideFullscreenButton(Landroid/widget/ImageView;)Landroid/widget/ImageView;
                    move-result-object v$insertRegister
                    if-nez v$insertRegister, :show
                    return-void
                    """, ExternalLabel("show", getInstruction(insertIndex))
            )
        }

        // endregion

        // region patch for hide previous and next button

        if (is_19_34_or_greater) {
            layoutConstructorFingerprint.methodOrThrow().apply {
                val resourceIndex =
                    indexOfFirstLiteralInstructionOrThrow(playerControlPreviousButtonTouchArea)

                val insertIndex = indexOfFirstInstructionOrThrow(resourceIndex) {
                    opcode == Opcode.INVOKE_STATIC &&
                            getReference<MethodReference>()?.parameterTypes?.firstOrNull() == "Landroid/view/View;"
                }

                val viewRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerC

                addInstruction(
                    insertIndex,
                    "invoke-static { v$viewRegister }, $PLAYER_CLASS_DESCRIPTOR" +
                            "->hidePreviousNextButtons(Landroid/view/View;)V",
            )
            }
        } else {
            PlayerControlsVisibilityModelFingerprint.method.apply {
                val callIndex = indexOfFirstInstructionOrThrow(Opcode.INVOKE_DIRECT_RANGE)
                val callInstruction = getInstruction<RegisterRangeInstruction>(callIndex)

                val hasNextParameterRegister = callInstruction.startRegister + HAS_NEXT
                val hasPreviousParameterRegister = callInstruction.startRegister + HAS_PREVIOUS

                addInstructions(
                    callIndex, """
                        invoke-static { v$hasNextParameterRegister }, $PLAYER_CLASS_DESCRIPTOR->hidePreviousNextButton(Z)Z
                        move-result v$hasNextParameterRegister
                        invoke-static { v$hasPreviousParameterRegister }, $PLAYER_CLASS_DESCRIPTOR->hidePreviousNextButton(Z)Z
                        move-result v$hasPreviousParameterRegister
                        """
                )
            }
        }

        // endregion

        // region Hide player control buttons background.

        inflateControlsGroupLayoutStubFingerprint.method.apply {
            val controlsButtonGroupLayoutStubResIdConstIndex =
                indexOfFirstLiteralInstructionOrThrow(youTubeControlsButtonGroupLayoutStub)
            val inflateControlsGroupLayoutStubIndex =
                indexOfFirstInstruction(controlsButtonGroupLayoutStubResIdConstIndex) {
                    getReference<MethodReference>()?.name == "inflate"
                }

            val freeRegister = findFreeRegister(inflateControlsGroupLayoutStubIndex)
            val hidePlayerControlButtonsBackgroundDescriptor =
                "$PLAYER_CLASS_DESCRIPTOR->hidePlayerControlButtonsBackground(Landroid/view/View;)V"

            addInstructions(
                inflateControlsGroupLayoutStubIndex + 1,
                """
                   # Move the inflated layout to a temporary register.
                   # The result of the inflate method is by default not moved to a register after the method is called.
                   move-result-object v$freeRegister
                   invoke-static { v$freeRegister }, $hidePlayerControlButtonsBackgroundDescriptor
                """
            )
        }

        // endregion

        // region patch for hide youtube music button

        MusicAppDeeplinkButtonFingerprint.match(MusicAppDeeplinkButtonParentFingerprint.classDef)
            .method.apply {
                addInstructionsWithLabels(
                    0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideMusicButton()Z
                    move-result v0
                    if-nez v0, :hidden
                    """,
                    ExternalLabel("hidden", getInstruction(implementation!!.instructions.lastIndex))
                )
            }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: HIDE_PLAYER_BUTTONS"
            ),
            HIDE_PLAYER_BUTTONS
        )

        // endregion

    }
}
