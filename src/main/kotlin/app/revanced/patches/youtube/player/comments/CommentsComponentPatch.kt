package app.revanced.patches.youtube.player.comments

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.shared.fingerprints.SpannableStringBuilderFingerprint
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.shared.textcomponent.TextComponentPatch
import app.revanced.patches.youtube.player.comments.fingerprints.ShortsLiveStreamEmojiPickerOnClickListenerFingerprint
import app.revanced.patches.youtube.player.comments.fingerprints.ShortsLiveStreamEmojiPickerOpacityFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.integrations.Constants.PLAYER_PATH
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getFiveRegisters
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object CommentsComponentPatch : BaseBytecodePatch(
    name = "Hide comments components",
    description = "Adds options to hide components related to comments.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        TextComponentPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        ShortsLiveStreamEmojiPickerOnClickListenerFingerprint,
        ShortsLiveStreamEmojiPickerOpacityFingerprint,
        SpannableStringBuilderFingerprint,
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/CommentsFilter;"
    private const val INTEGRATIONS_SEARCH_LINKS_CLASS_DESCRIPTOR =
        "$PLAYER_PATH/SearchLinksPatch;"

    override fun execute(context: BytecodeContext) {

        TextComponentPatch.hookSpannableString(
            INTEGRATIONS_SEARCH_LINKS_CLASS_DESCRIPTOR,
            "setConversionContext"
        )

        SpannableStringBuilderFingerprint.resultOrThrow().mutableMethod.apply {
            val spannedIndex =
                SpannableStringBuilderFingerprint.indexOfSpannableStringInstruction(this)
            val setInclusiveSpanIndex = indexOfFirstInstructionOrThrow(spannedIndex) {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_STATIC &&
                        reference?.returnType == "V" &&
                        reference.parameterTypes.size > 3 &&
                        reference.parameterTypes.firstOrNull() == "Landroid/text/SpannableString;"
            }
            // In YouTube 18.29.38, YouTube 19.41.39, the target method is in class 'La;'
            // 'getWalkerMethod' should be used until the dependency is updated to ReVanced Patcher 20+.
            // https://github.com/ReVanced/revanced-patcher/issues/309
            val setInclusiveSpanMethod =
                getWalkerMethod(context, setInclusiveSpanIndex)

            setInclusiveSpanMethod.apply {
                val insertIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>().toString() == "Landroid/text/SpannableString;->setSpan(Ljava/lang/Object;III)V"
                }
                replaceInstruction(
                    insertIndex,
                    "invoke-static { ${getFiveRegisters(insertIndex)} }, " +
                            INTEGRATIONS_SEARCH_LINKS_CLASS_DESCRIPTOR +
                            "->" +
                            "hideSearchLinks(Landroid/text/SpannableString;Ljava/lang/Object;III)V"
                )
            }
        }

        // region patch for emoji picker button in shorts

        ShortsLiveStreamEmojiPickerOpacityFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->changeEmojiPickerOpacity(Landroid/widget/ImageView;)V"
                )
            }
        }

        ShortsLiveStreamEmojiPickerOnClickListenerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val emojiPickerEndpointIndex =
                    indexOfFirstWideLiteralInstructionValueOrThrow(126326492)
                val emojiPickerOnClickListenerIndex =
                    indexOfFirstInstructionOrThrow(emojiPickerEndpointIndex, Opcode.INVOKE_DIRECT)
                val emojiPickerOnClickListenerMethod =
                    getWalkerMethod(context, emojiPickerOnClickListenerIndex)

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
        }

        // endregion

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_COMMENTS_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
