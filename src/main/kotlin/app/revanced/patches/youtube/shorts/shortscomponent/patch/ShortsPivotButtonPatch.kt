package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsPivotFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelForcedMuteButton
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class ShortsPivotButtonPatch : BytecodePatch(
    listOf(ShortsPivotFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ShortsPivotFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralIndex(ReelForcedMuteButton)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                val insertIndex = getTargetIndexDownTo(targetIndex, Opcode.IF_EQZ)
                val jumpIndex = getTargetIndexUpTo(targetIndex, Opcode.GOTO)

                if (insertIndex == -1 || jumpIndex == -1)
                    throw PatchException("Failed to find hook method")

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $SHORTS->hideShortsPlayerPivotButton()Z
                        move-result v$targetRegister
                        if-nez v$targetRegister, :hide
                        """, ExternalLabel("hide", getInstruction(jumpIndex))
                )
            }
        } ?: throw ShortsPivotFingerprint.exception

    }
    private companion object {
        fun MutableMethod.getTargetIndexDownTo(
            startIndex: Int,
            opcode: Opcode
        ): Int {
            for (index in startIndex downTo 0) {
                if (getInstruction(index).opcode != opcode)
                    continue

                return index
            }
            return -1
        }

        fun MutableMethod.getTargetIndexUpTo(
            startIndex: Int,
            opcode: Opcode
        ): Int {
            for (index in startIndex until implementation!!.instructions.size) {
                if (getInstruction(index).opcode != opcode)
                    continue

                return index
            }
            return -1
        }
    }
}
