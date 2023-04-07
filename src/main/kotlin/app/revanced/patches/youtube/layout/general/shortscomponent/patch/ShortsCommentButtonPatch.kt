package app.revanced.patches.youtube.layout.general.shortscomponent.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.shortscomponent.fingerprints.ShortsCommentFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction

@Name("hide-shorts-comment")
@DependsOn([SharedResourceIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ShortsCommentButtonPatch : BytecodePatch(
    listOf(ShortsCommentFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsCommentFingerprint.result?.mutableMethod?.let { method ->
            with (method.implementation!!.instructions) {
                val insertIndex = this.indexOfFirst {
                    (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.rightCommentLabelId
                } + 3

                val insertRegister = (elementAt(insertIndex) as OneRegisterInstruction).registerA

                method.addInstruction(
                    insertIndex + 1,
                    "invoke-static {v$insertRegister}, $GENERAL->hideShortsPlayerCommentsButton(Landroid/view/View;)V"
                )
            }
        } ?: return ShortsCommentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
