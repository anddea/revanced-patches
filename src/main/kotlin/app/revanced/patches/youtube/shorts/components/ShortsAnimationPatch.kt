package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.shorts.components.fingerprints.ReelFeedbackFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.SHORTS_PATH
import app.revanced.patches.youtube.utils.lottie.LottieAnimationViewHookPatch
import app.revanced.patches.youtube.utils.lottie.fingerprints.SetAnimationFingerprint.LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelFeedbackLike
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelFeedbackPause
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelFeedbackPlay
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(dependencies = [LottieAnimationViewHookPatch::class])
object ShortsAnimationPatch : BytecodePatch(
    setOf(ReelFeedbackFingerprint)
) {
    private const val INTEGRATION_CLASS_DESCRIPTOR =
        "$SHORTS_PATH/AnimationFeedbackPatch;"

    override fun execute(context: BytecodeContext) {

        ReelFeedbackFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                mapOf(
                    ReelFeedbackLike to "setShortsLikeFeedback",
                    ReelFeedbackPause to "setShortsPauseFeedback",
                    ReelFeedbackPlay to "setShortsPlayFeedback",
                ).forEach { (literal, methodName) ->
                    val literalIndex = getWideLiteralInstructionIndex(literal)
                    val viewIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                        opcode == Opcode.CHECK_CAST
                                && (this as? ReferenceInstruction)?.reference?.toString() == LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR
                    }
                    val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA
                    val methodCall = "invoke-static {v$viewRegister}, " +
                            INTEGRATION_CLASS_DESCRIPTOR +
                            "->" +
                            methodName +
                            "($LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR)V"

                    addInstruction(
                        viewIndex + 1,
                        methodCall
                    )
                }
            }
        }

        /**
         * Copy json
         */
        contexts.copyResources(
            "youtube/shorts/feedback",
            ResourceGroup(
                "raw",
                "like_tap_feedback_cairo.json",
                "like_tap_feedback_heart.json",
                "like_tap_feedback_heart_tint.json",
                "like_tap_feedback_hidden.json",
                "pause_tap_feedback_hidden.json",
                "play_tap_feedback_hidden.json"
            )
        )
    }
}
