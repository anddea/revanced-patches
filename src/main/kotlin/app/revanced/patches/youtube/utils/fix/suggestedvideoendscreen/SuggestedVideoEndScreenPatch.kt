package app.revanced.patches.youtube.utils.fix.suggestedvideoendscreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.fix.suggestedvideoendscreen.fingerprints.RemoveOnLayoutChangeListenerFingerprint
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getWalkerMethod
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Patch(
    description = "Fixes an issue where the suggested video end screen is always visible regardless of whether autoplay is set or not."
)
object SuggestedVideoEndScreenPatch : BytecodePatch(
    setOf(RemoveOnLayoutChangeListenerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * The reasons why this patch is classified as a patch that fixes a 'bug' are as follows:
         * 1. In YouTube v18.29.38, the suggested video end screen was only shown when the autoplay setting was turned on.
         * 2. Starting from YouTube v18.35.36, the suggested video end screen is shown regardless of whether autoplay setting was turned on or off.
         *
         * This patch changes the suggested video end screen to be shown only when the autoplay setting is turned on.
         * Automatically closing the suggested video end screen is not appropriate as it will disable the autoplay behavior.
         */
        RemoveOnLayoutChangeListenerFingerprint.resultOrThrow().let {
            val walkerIndex = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.endIndex)

            walkerIndex.apply {
                val invokeInterfaceIndex = getTargetIndex(Opcode.INVOKE_INTERFACE)
                val iGetObjectIndex = getTargetIndexReversed(invokeInterfaceIndex, Opcode.IGET_OBJECT)

                val invokeInterfaceReference = getInstruction<ReferenceInstruction>(invokeInterfaceIndex).reference
                val iGetObjectReference = getInstruction<ReferenceInstruction>(iGetObjectIndex).reference

                addInstructionsWithLabels(
                    0,
                    """
                        iget-object v0, p0, $iGetObjectReference

                        # This reference checks whether autoplay is turned on.
                        invoke-interface {v0}, $invokeInterfaceReference
                        move-result v0

                        # Hide suggested video end screen only when autoplay is turned off.
                        if-nez v0, :show_suggested_video_end_screen
                        return-void
                        """,
                    ExternalLabel(
                        "show_suggested_video_end_screen",
                        getInstruction(0)
                    )
                )
            }
        }

    }
}
