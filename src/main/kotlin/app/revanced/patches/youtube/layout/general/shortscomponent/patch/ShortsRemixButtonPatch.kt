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
import app.revanced.patches.youtube.layout.general.shortscomponent.fingerprints.ShortsRemixFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.GENERAL
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction

@Name("hide-shorts-remix")
@DependsOn([SharedResourceIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ShortsRemixButtonPatch : BytecodePatch(
    listOf(ShortsRemixFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsRemixFingerprint.result?.mutableMethod?.let { method ->
            with (method.implementation!!.instructions) {
                val insertIndex = this.indexOfFirst {
                    (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.reelRemixLabelId
                } - 2

                val insertRegister = (elementAt(insertIndex) as OneRegisterInstruction).registerA

                method.addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $GENERAL->hideShortsPlayerRemixButton(Landroid/view/View;)V"
                )
            }
        } ?: return ShortsRemixFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
