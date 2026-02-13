package app.morphe.patches.youtube.player.comments

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.comments.commentsPanelPatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.spans.addSpanFilter
import app.morphe.patches.shared.spans.inclusiveSpanPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.componentlist.hookElementList
import app.morphe.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.SPANS_PATH
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_COMMENTS_COMPONENTS
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val COMMENTS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/CommentsFilter;"
private const val SEARCH_LINKS_FILTER_CLASS_DESCRIPTOR =
    "$SPANS_PATH/SearchLinksFilter;"

@Suppress("unused")
val commentsComponentPatch = bytecodePatch(
    HIDE_COMMENTS_COMPONENTS.title,
    HIDE_COMMENTS_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        inclusiveSpanPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        lazilyConvertedElementHookPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        commentsPanelPatch,
    )

    execute {

        // region patch for emoji picker button in shorts

        shortsLiveStreamEmojiPickerOpacityFingerprint.methodOrThrow().apply {
            val insertIndex = implementation!!.instructions.lastIndex
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstruction(
                insertIndex,
                "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->changeEmojiPickerOpacity(Landroid/widget/ImageView;)V"
            )
        }

        shortsLiveStreamEmojiPickerOnClickListenerFingerprint.methodOrThrow().apply {
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

        addSpanFilter(SEARCH_LINKS_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(COMMENTS_FILTER_CLASS_DESCRIPTOR)
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
