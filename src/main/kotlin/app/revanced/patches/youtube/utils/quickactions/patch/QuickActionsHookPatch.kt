package app.revanced.patches.youtube.utils.quickactions.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.utils.quickactions.fingerprints.QuickActionsElementFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.FULLSCREEN
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@DependsOn([SharedResourceIdPatch::class])
class QuickActionsHookPatch : BytecodePatch(
    listOf(QuickActionsElementFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        QuickActionsElementFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$targetRegister}, $FULLSCREEN->hideQuickActions(Landroid/view/View;)V"
                )
            }
        } ?: throw QuickActionsElementFingerprint.exception

    }
}
