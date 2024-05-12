package app.revanced.patches.youtube.utils.fix.fullscreen

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fix.fullscreen.fingerprints.FullscreenButtonViewStubFingerprint
import app.revanced.util.getTargetIndex
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    description = "Fixes an issue where overlay button patches were broken by the new layout."
)
object FullscreenButtonViewStubPatch : BytecodePatch(
    setOf(FullscreenButtonViewStubFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * This issue only affects some versions of YouTube.
         * Therefore, this patch only applies to versions that can resolve this fingerprint.
         */
        FullscreenButtonViewStubFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getTargetIndex(getWideLiteralInstructionIndex(45617294), Opcode.MOVE_RESULT)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "const/4 v$targetRegister, 0x0"
                )
            }
        }

    }
}
