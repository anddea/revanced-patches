package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsCommentFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.RightComment
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

class ShortsCommentButtonPatch : BytecodePatch(
    listOf(ShortsCommentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsCommentFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getWideLiteralIndex(RightComment) + 3
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerCommentsButton(Landroid/view/View;)V"
                )
            }
        } ?: return ShortsCommentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
