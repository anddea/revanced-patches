package app.revanced.patches.youtube.utils.fix.shortsplayback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fix.shortsplayback.fingerprints.ShortsPlaybackFingerprint
import app.revanced.util.getTargetIndex
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    description = "Fix issue with looping at the start of the video when applying default video quality to Shorts."
)
object ShortsPlaybackPatch : BytecodePatch(
    setOf(ShortsPlaybackFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * This issue only affects some versions of YouTube.
         * Therefore, this patch only applies to versions that can resolve this fingerprint.
         *
         * RVX applies default video quality to Shorts as well, so this patch is required.
         */
        ShortsPlaybackFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndex(getWideLiteralInstructionIndex(45387052), Opcode.MOVE_RESULT)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "const/4 v$targetRegister, 0x0"
                )
            }
        }

    }
}
