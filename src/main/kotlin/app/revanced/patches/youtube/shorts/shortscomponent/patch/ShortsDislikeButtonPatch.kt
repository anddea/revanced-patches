package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsDislikeFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelRightDislikeIcon
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class ShortsDislikeButtonPatch : BytecodePatch(
    listOf(ShortsDislikeFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ShortsDislikeFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getWideLiteralIndex(ReelRightDislikeIcon)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                for (index in insertIndex until implementation!!.instructions.size) {
                    if (getInstruction(index).opcode != Opcode.CONST_CLASS) continue

                    addInstructionsWithLabels(
                        insertIndex + 1, """
                            invoke-static {}, $SHORTS->hideShortsPlayerDislikeButton()Z
                            move-result v$insertRegister
                            if-nez v$insertRegister, :hide
                            const v$insertRegister, $ReelRightDislikeIcon
                            """, ExternalLabel("hide", getInstruction(index + 2))
                    )
                    break
                }
            }
        } ?: throw ShortsDislikeFingerprint.exception

    }
}
