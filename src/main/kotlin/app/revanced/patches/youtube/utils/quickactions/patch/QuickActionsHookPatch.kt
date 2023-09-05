package app.revanced.patches.youtube.utils.quickactions.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.utils.quickactions.fingerprints.QuickActionsElementFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.QuickActionsElementContainer
import app.revanced.util.integrations.Constants.FULLSCREEN
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction

@DependsOn([SharedResourceIdPatch::class])
class QuickActionsHookPatch : BytecodePatch(
    listOf(QuickActionsElementFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        QuickActionsElementFingerprint.result?.let {
            it.mutableMethod.apply {
                for (index in implementation!!.instructions.size - 1 downTo 0) {
                    if (getInstruction(index).opcode == Opcode.CONST && (getInstruction(index) as WideLiteralInstruction).wideLiteral == QuickActionsElementContainer) {
                        val targetRegister =
                            getInstruction<OneRegisterInstruction>(index + 2).registerA

                        addInstruction(
                            index + 3,
                            "invoke-static {v$targetRegister}, $FULLSCREEN->hideQuickActions(Landroid/view/View;)V"
                        )
                        break
                    }
                }
            }
        } ?: throw QuickActionsElementFingerprint.exception

    }
}
