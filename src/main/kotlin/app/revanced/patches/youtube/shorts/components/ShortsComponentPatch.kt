package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsButtonFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsPaidPromotionFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsPivotFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsPivotLegacyFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsSubscriptionsTabletFingerprint
import app.revanced.patches.youtube.shorts.components.fingerprints.ShortsSubscriptionsTabletParentFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fingerprints.TextComponentSpecFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelDynRemix
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelDynShare
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelForcedMuteButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelPivotButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelRightDislikeIcon
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelRightLikeIcon
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.RightComment
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.video.information.VideoInformationPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
object ShortsComponentPatch : BaseBytecodePatch(
    name = "Shorts components",
    description = "Adds options to hide or change components related to YouTube Shorts.",
    dependencies = setOf(
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        ShortsNavigationBarPatch::class,
        ShortsToolBarPatch::class,
        VideoInformationPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        ShortsButtonFingerprint,
        ShortsPaidPromotionFingerprint,
        ShortsPivotFingerprint,
        ShortsPivotLegacyFingerprint,
        ShortsSubscriptionsTabletParentFingerprint,
        TextComponentSpecFingerprint
    )
) {
    private const val INTEGRATION_CLASS_DESCRIPTOR =
        "$UTILS_PATH/ReturnYouTubeChannelNamePatch;"

    private const val BUTTON_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ShortsButtonFilter;"
    private const val SHELF_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ShortsShelfFilter;"
    private const val RETURN_YOUTUBE_CHANNEL_NAME_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/ReturnYouTubeChannelNameFilterPatch;"

    override fun execute(context: BytecodeContext) {

        // region patch for hide comments button (non-litho)

        ShortsButtonFingerprint.hideButton(RightComment, "hideShortsCommentsButton", false)

        // endregion

        // region patch for hide dislike button (non-litho)

        ShortsButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(ReelRightDislikeIcon)
                val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA

                val jumpIndex = getTargetIndex(constIndex, Opcode.CONST_CLASS) + 2

                addInstructionsWithLabels(
                    constIndex + 1, """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsDislikeButton()Z
                        move-result v$constRegister
                        if-nez v$constRegister, :hide
                        const v$constRegister, $ReelRightDislikeIcon
                        """, ExternalLabel("hide", getInstruction(jumpIndex))
                )
            }
        }

        // endregion

        // region patch for hide like button (non-litho)

        ShortsButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getWideLiteralInstructionIndex(ReelRightLikeIcon)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA
                val jumpIndex = getTargetIndex(insertIndex, Opcode.CONST_CLASS) + 2

                addInstructionsWithLabels(
                    insertIndex + 1, """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsLikeButton()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :hide
                        const v$insertRegister, $ReelRightLikeIcon
                        """, ExternalLabel("hide", getInstruction(jumpIndex))
                )
            }
        }

        // endregion

        // region patch for hide sound button

        ShortsPivotLegacyFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(ReelForcedMuteButton)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                val insertIndex = getTargetIndexReversed(targetIndex, Opcode.IF_EQZ)
                val jumpIndex = getTargetIndex(targetIndex, Opcode.GOTO)

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsSoundButton()Z
                        move-result v$targetRegister
                        if-nez v$targetRegister, :hide
                        """, ExternalLabel("hide", getInstruction(jumpIndex))
                )
            }
        } ?: ShortsPivotFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(ReelPivotButton)
                val insertIndex = getTargetIndexReversed(targetIndex, Opcode.INVOKE_STATIC) + 1

                hideButtons(insertIndex, "hideShortsSoundButton(Ljava/lang/Object;)Ljava/lang/Object;")
            }
        }

        // endregion

        // region patch for hide remix button (non-litho)

        ShortsButtonFingerprint.hideButton(ReelDynRemix, "hideShortsRemixButton", true)

        // endregion

        // region patch for hide share button (non-litho)

        ShortsButtonFingerprint.hideButton(ReelDynShare, "hideShortsShareButton", true)

        // endregion

        // region patch for hide paid promotion label (non-litho)

        ShortsPaidPromotionFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                when (returnType) {
                    "Landroid/widget/TextView;" -> {
                        val insertIndex = implementation!!.instructions.size - 1
                        val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                        addInstructions(
                            insertIndex + 1, """
                                invoke-static {v$insertRegister}, $SHORTS_CLASS_DESCRIPTOR->hideShortsPaidPromotionLabel(Landroid/widget/TextView;)V
                                return-object v$insertRegister
                                """
                        )
                        removeInstruction(insertIndex)
                    }
                    "V" -> {
                        addInstructionsWithLabels(
                            0, """
                                invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsPaidPromotionLabel()Z
                                move-result v0
                                if-eqz v0, :show
                                return-void
                                """, ExternalLabel("show", getInstruction(0))
                        )
                    }
                    else -> {
                        throw PatchException("Unknown returnType: $returnType")
                    }
                }
            }
        }

        // endregion

        // region patch for hide subscribe button (non-litho)

        // This method is deprecated since YouTube v18.31.xx.
        if (!SettingsPatch.upward1831) {
            ShortsSubscriptionsTabletParentFingerprint.resultOrThrow().let { parentResult ->
                lateinit var subscriptionFieldReference: FieldReference

                parentResult.mutableMethod.apply {
                    val targetIndex = getWideLiteralInstructionIndex(SharedResourceIdPatch.ReelPlayerFooter) - 1
                    subscriptionFieldReference =
                        (getInstruction<ReferenceInstruction>(targetIndex)).reference as FieldReference
                }

                ShortsSubscriptionsTabletFingerprint.also {
                    it.resolve(
                        context,
                        parentResult.classDef
                    )
                }.resultOrThrow().mutableMethod.apply {
                    implementation!!.instructions.filter { instruction ->
                        val fieldReference =
                            (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        instruction.opcode == Opcode.IGET
                                && fieldReference == subscriptionFieldReference
                    }.forEach { instruction ->
                        val insertIndex = implementation!!.instructions.indexOf(instruction) + 1
                        val register = (instruction as TwoRegisterInstruction).registerA

                        addInstructions(
                            insertIndex, """
                                invoke-static {v$register}, $SHORTS_CLASS_DESCRIPTOR->hideShortsSubscribeButton(I)I
                                move-result v$register
                                """
                        )
                    }
                }
            }
        }

        // endregion

        // region patch for return shorts channel name

        TextComponentSpecFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndexWithReference("Landroid/text/SpannableString;->valueOf(Ljava/lang/CharSequence;)Landroid/text/SpannableString;")

                val charSequenceRegister =
                    getInstruction<FiveRegisterInstruction>(insertIndex).registerC
                val conversionContextRegister =
                    getInstruction<TwoRegisterInstruction>(0).registerA

                val replaceReference =
                    getInstruction<ReferenceInstruction>(insertIndex).reference

                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$conversionContextRegister, v$charSequenceRegister}, $INTEGRATION_CLASS_DESCRIPTOR->onCharSequenceLoaded(Ljava/lang/Object;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$charSequenceRegister
                        invoke-static {v$charSequenceRegister}, $replaceReference
                        """
                )
                removeInstruction(insertIndex)
            }
        }

        VideoInformationPatch.hookShorts("$INTEGRATION_CLASS_DESCRIPTOR->newShortsVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        // endregion

        LithoFilterPatch.addFilter(BUTTON_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(SHELF_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(RETURN_YOUTUBE_CHANNEL_NAME_FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SHORTS",
                "SETTINGS: SHORTS_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }

    private fun MethodFingerprint.hideButton(
        id: Long,
        descriptor: String,
        reversed: Boolean
    ) {
        resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(id)
                val insertIndex = if (reversed)
                    getTargetIndexReversed(constIndex, Opcode.CHECK_CAST)
                else
                    getTargetIndex(constIndex, Opcode.CHECK_CAST)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $SHORTS_CLASS_DESCRIPTOR->$descriptor(Landroid/view/View;)V"
                )
            }
        }
    }

    private fun MethodFingerprint.hideButtons(
        id: Long,
        descriptor: String
    ) {
        resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(id)
                val insertIndex = getTargetIndex(constIndex, Opcode.CHECK_CAST)

                hideButtons(insertIndex, descriptor)
            }
        }
    }

    private fun MutableMethod.hideButtons(
        insertIndex: Int,
        descriptor: String
    ) {
        val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

        addInstructions(
            insertIndex + 1, """
                invoke-static {v$insertRegister}, $SHORTS_CLASS_DESCRIPTOR->$descriptor
                move-result-object v$insertRegister
                """
        )
    }
}
