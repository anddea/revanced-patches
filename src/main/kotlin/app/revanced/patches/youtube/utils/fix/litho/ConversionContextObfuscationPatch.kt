package app.revanced.patches.youtube.utils.fix.litho

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fix.litho.fingerprints.ObfuscationConfigFingerprint
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    description = "Fix the issue where ConversionContext is obfuscating. "
            + "When ConversionContext is obfuscated, most patches are broken because the litho components can no longer be identified."
)
object ConversionContextObfuscationPatch : BytecodePatch(
    setOf(ObfuscationConfigFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * I found a boolean value on YouTube 19.19.39 that obfuscates ConversionContext,
         * but I'm not sure if this is for testing purposes only.
         */
        ObfuscationConfigFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex =
                    getTargetIndexOrThrow(
                        getWideLiteralInstructionIndex(45631264),
                        Opcode.MOVE_RESULT
                    )
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "const/4 v$targetRegister, 0x0"
                )
            }
        }

    }
}
