package app.revanced.patches.youtube.player.buttons

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.buttons.fingerprints.FullScreenButtonFingerprint
import app.revanced.patches.youtube.player.buttons.fingerprints.LithoSubtitleButtonConfigFingerprint
import app.revanced.patches.youtube.player.buttons.fingerprints.MusicAppDeeplinkButtonFingerprint
import app.revanced.patches.youtube.player.buttons.fingerprints.MusicAppDeeplinkButtonParentFingerprint
import app.revanced.patches.youtube.player.buttons.fingerprints.PlayerControlsVisibilityModelFingerprint
import app.revanced.patches.youtube.player.buttons.fingerprints.TitleAnchorFingerprint
import app.revanced.patches.youtube.player.buttons.fingerprints.YouTubeControlsOverlaySubtitleButtonFingerprint
import app.revanced.patches.youtube.utils.castbutton.CastButtonPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fingerprints.LayoutConstructorFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AutoNavToggle
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.PlayerCollapseButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.TitleAnchor
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc

@Suppress("unused")
object PlayerButtonsPatch : BaseBytecodePatch(
    name = "Hide player buttons",
    description = "Adds an option to hide buttons in the video player.",
    dependencies = setOf(
        CastButtonPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        FullScreenButtonFingerprint,
        LayoutConstructorFingerprint,
        LithoSubtitleButtonConfigFingerprint,
        MusicAppDeeplinkButtonParentFingerprint,
        PlayerControlsVisibilityModelFingerprint,
        TitleAnchorFingerprint,
        YouTubeControlsOverlaySubtitleButtonFingerprint,
    )
) {
    private const val HAS_NEXT = 5
    private const val HAS_PREVIOUS = 6

    override fun execute(context: BytecodeContext) {

        // region patch for hide autoplay button

        LayoutConstructorFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(AutoNavToggle)
                val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA
                val jumpIndex = getTargetIndex(constIndex + 2, Opcode.INVOKE_VIRTUAL) + 1

                addInstructionsWithLabels(
                    constIndex, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideAutoPlayButton()Z
                        move-result v$constRegister
                        if-nez v$constRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        }

        // endregion

        // region patch for hide captions button

        /**
         * Added in YouTube v18.31.40
         *
         * No exception even if fail to resolve fingerprints.
         * For compatibility with YouTube v18.25.40 ~ YouTube v18.30.37.
         */
        LithoSubtitleButtonConfigFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hideCaptionsButton(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }

        YouTubeControlsOverlaySubtitleButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hideCaptionsButton(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide cast button

        CastButtonPatch.hookPlayerButton(context)

        // endregion

        // region patch for hide collapse button

        TitleAnchorFingerprint.resultOrThrow().mutableMethod.apply {
            val titleAnchorConstIndex = getWideLiteralInstructionIndex(TitleAnchor)
            val titleAnchorIndex = getTargetIndex(titleAnchorConstIndex, Opcode.MOVE_RESULT_OBJECT)
            val titleAnchorRegister = getInstruction<OneRegisterInstruction>(titleAnchorIndex).registerA

            addInstruction(
                titleAnchorIndex + 1,
                "invoke-static {v$titleAnchorRegister}, $PLAYER_CLASS_DESCRIPTOR->setTitleAnchorStartMargin(Landroid/view/View;)V"
            )

            val playerCollapseButtonConstIndex = getWideLiteralInstructionIndex(PlayerCollapseButton)
            val playerCollapseButtonIndex = getTargetIndex(playerCollapseButtonConstIndex, Opcode.CHECK_CAST)
            val playerCollapseButtonRegister = getInstruction<OneRegisterInstruction>(playerCollapseButtonIndex).registerA

            addInstruction(
                playerCollapseButtonIndex + 1,
                "invoke-static {v$playerCollapseButtonRegister}, $PLAYER_CLASS_DESCRIPTOR->hideCollapseButton(Landroid/widget/ImageView;)V"
            )
        }

        // endregion

        // region patch for hide fullscreen button

        FullScreenButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val viewIndex = getTargetIndexWithReference("Landroid/widget/ImageView;->getResources()Landroid/content/res/Resources;")
                val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerC

                addInstructionsWithLabels(
                    viewIndex, """
                        invoke-static {v$viewRegister}, $PLAYER_CLASS_DESCRIPTOR->hideFullscreenButton(Landroid/widget/ImageView;)Landroid/widget/ImageView;
                        move-result-object v$viewRegister
                        if-nez v$viewRegister, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(viewIndex))
                )
            }
        }

        // endregion

        // region patch for hide previous and next button

        PlayerControlsVisibilityModelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val callIndex = getTargetIndex(Opcode.INVOKE_DIRECT_RANGE)
                val callInstruction = getInstruction<Instruction3rc>(callIndex)

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

        // region patch for hide youtube music button

        MusicAppDeeplinkButtonFingerprint.resolve(
            context,
            MusicAppDeeplinkButtonParentFingerprint.resultOrThrow().mutableClass
        )
        MusicAppDeeplinkButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideMusicButton()Z
                        move-result v0
                        if-nez v0, :hidden
                        """,
                    ExternalLabel("hidden", getInstruction(implementation!!.instructions.size - 1))
                )
            }
        }

        // endregion


        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: HIDE_PLAYER_BUTTONS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}

