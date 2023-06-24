package app.revanced.patches.youtube.utils.quickactions.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.quickactions.fingerprints.QuickActionsElementFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.FULLSCREEN
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("quick-actions-hook")
@DependsOn([SharedResourceIdPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class QuickActionsHookPatch : BytecodePatch(
    listOf(QuickActionsElementFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        QuickActionsElementFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$targetRegister}, $FULLSCREEN->hideQuickActions(Landroid/view/View;)V"
                )
            }
        } ?: return QuickActionsElementFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
