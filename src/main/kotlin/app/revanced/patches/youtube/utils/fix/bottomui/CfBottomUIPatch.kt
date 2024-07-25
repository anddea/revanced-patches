package app.revanced.patches.youtube.utils.fix.bottomui

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fix.bottomui.fingerprints.BottomUIContainerBooleanFingerprint
import app.revanced.patches.youtube.utils.fix.bottomui.fingerprints.BottomUIContainerIntegerFingerprint
import app.revanced.patches.youtube.utils.fix.bottomui.fingerprints.FullscreenButtonPositionFingerprint
import app.revanced.patches.youtube.utils.fix.bottomui.fingerprints.FullscreenButtonViewStubFingerprint
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    description = "Fixes an issue where overlay button patches were broken by the new layout."
)
object CfBottomUIPatch : BytecodePatch(
    setOf(
        BottomUIContainerBooleanFingerprint,
        BottomUIContainerIntegerFingerprint,
        FullscreenButtonPositionFingerprint,
        FullscreenButtonViewStubFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * This issue only affects some versions of YouTube.
         * Therefore, this patch only applies to versions that can resolve this fingerprint.
         */
        mapOf(
            BottomUIContainerBooleanFingerprint to 45637647,
            BottomUIContainerIntegerFingerprint to 45637647,
            FullscreenButtonViewStubFingerprint to 45617294,
            FullscreenButtonPositionFingerprint to 45627640
        ).forEach { (fingerprint, literalValue) ->
            fingerprint.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = getTargetIndexOrThrow(
                        getWideLiteralInstructionIndex(literalValue.toLong()),
                        Opcode.MOVE_RESULT
                    )
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstruction(
                        targetIndex + 1,
                        "const/4 v$targetRegister, 0x0"
                    )
                }
            }
        }

    }
}
