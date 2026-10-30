/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.player.comments

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.cloneParameters
import app.morphe.patcher.util.getReference
import app.morphe.patches.shared.comments.commentsPanelPatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.spans.addSpanFilter
import app.morphe.patches.shared.spans.inclusiveSpanPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.componentlist.hookElementList
import app.morphe.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.SPANS_PATH
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_COMMENTS_COMPONENTS
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.proto.elementProtoParserHookPatch
import app.morphe.patches.youtube.utils.proto.hookElement
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val COMMENTS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/CommentsFilter;"
private const val SEARCH_LINKS_FILTER_CLASS_DESCRIPTOR =
    "$SPANS_PATH/SearchLinksFilter;"

@Suppress("unused")
val commentsComponentPatch = bytecodePatch(
    HIDE_COMMENTS_COMPONENTS.title,
    HIDE_COMMENTS_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        inclusiveSpanPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        elementProtoParserHookPatch,
        lazilyConvertedElementHookPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        commentsPanelPatch,
    )

    execute {

        // region patch for emoji picker button in shorts

        ShortsLiveStreamEmojiPickerOpacityFingerprint.method.apply {
            val insertIndex = implementation!!.instructions.lastIndex
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex,
                "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->changeEmojiPickerOpacity(Landroid/widget/ImageView;)V"
            )
        }

        ShortsLiveStreamEmojiPickerOnClickListenerFingerprint.method.apply {
            val emojiPickerEndpointIndex =
                indexOfFirstLiteralInstructionOrThrow(126326492L)
            val emojiPickerOnClickListenerIndex =
                indexOfFirstInstructionOrThrow(emojiPickerEndpointIndex, Opcode.INVOKE_DIRECT)
            val emojiPickerOnClickListenerMethod =
                getWalkerMethod(emojiPickerOnClickListenerIndex)

            emojiPickerOnClickListenerMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IF_EQZ)
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->disableEmojiPickerOnClickListener(Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$insertRegister
                        """
                )
            }
        }

        // endregion

        // region hide category bar in comments

        PanelSubheaderFingerprint.let {
            it.method.apply {
                val removeAllViewsIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "removeAllViews"
                }

                val setVisibilityIndex = indexOfFirstInstructionOrThrow(removeAllViewsIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setVisibility"
                }

                val subheaderRegister = getInstruction<FiveRegisterInstruction>(setVisibilityIndex).registerC

                addInstruction(
                    setVisibilityIndex + 1,
                    "invoke-static { v$subheaderRegister }, $COMMENTS_FILTER_CLASS_DESCRIPTOR->hideInComments(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region hide guidelines sheet shown before commenting

        // The parameter register is free after it's copied, and keeps the field number of the payload.
        // The copy is the first instruction, so the indexes are found again.
        EntityMutationPayloadFingerprint.method.cloneParameters().apply {
            val tagIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT)
            val tagRegister = getInstruction<OneRegisterInstruction>(tagIndex).registerA
            val returnIndex = indexOfFirstInstructionOrThrow(Opcode.RETURN_OBJECT)
            val payloadRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA

            addInstructions(
                returnIndex,
                """
                    ushr-int/lit8 p0, p0, 0x3
                    invoke-static { p0, v$payloadRegister }, $COMMENTS_FILTER_CLASS_DESCRIPTOR->hideGuidelinesSheet(I[B)[B
                    move-result-object v$payloadRegister
                """
            )
            addInstruction(tagIndex + 1, "move p0, v$tagRegister")
        }

        // endregion

        addSpanFilter(SEARCH_LINKS_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(COMMENTS_FILTER_CLASS_DESCRIPTOR)
        hookElement("$COMMENTS_FILTER_CLASS_DESCRIPTOR->onCommentsLoaded([B)[B")
        hookElementList("$PLAYER_CLASS_DESCRIPTOR->sanitizeCommentsCategoryBar")

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_COMMENTS_COMPONENTS"
            ),
            HIDE_COMMENTS_COMPONENTS
        )

        // endregion

    }
}
