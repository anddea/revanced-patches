package app.revanced.patches.youtube.layout.shorts.shortscomponent.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.shorts.shortscomponent.fingerprints.ShortsCommentFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.rightCommentId
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("hide-shorts-comment")
@YouTubeCompatibility
@Version("0.0.1")
class ShortsCommentButtonPatch : BytecodePatch(
    listOf(ShortsCommentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsCommentFingerprint.result?.mutableMethod?.let {
            val insertIndex = it.getWideLiteralIndex(rightCommentId) + 3
            val insertRegister = it.getInstruction<OneRegisterInstruction>(insertIndex).registerA

            it.addInstruction(
                insertIndex + 1,
                "invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerCommentsButton(Landroid/view/View;)V"
            )
        } ?: return ShortsCommentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
