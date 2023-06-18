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
import app.revanced.patches.youtube.layout.shorts.shortscomponent.fingerprints.ShortsRemixFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.reelRemixId
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("hide-shorts-remix")
@YouTubeCompatibility
@Version("0.0.1")
class ShortsRemixButtonPatch : BytecodePatch(
    listOf(ShortsRemixFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsRemixFingerprint.result?.mutableMethod?.let {
            val insertIndex = it.getWideLiteralIndex(reelRemixId) - 2
            val insertRegister = it.getInstruction<OneRegisterInstruction>(insertIndex).registerA

            it.addInstruction(
                insertIndex,
                "invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerRemixButton(Landroid/view/View;)V"
            )
        } ?: return ShortsRemixFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
