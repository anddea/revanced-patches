package app.revanced.patches.music.utils.actionbarhook

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.actionbarhook.fingerprints.ActionBarHookFingerprint
import app.revanced.patches.music.utils.fingerprints.ActionsBarParentFingerprint
import app.revanced.patches.music.utils.integrations.Constants.ACTIONBAR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Patch(dependencies = [SharedResourceIdPatch::class])
object ActionBarHookPatch : BytecodePatch(
    setOf(ActionsBarParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ActionsBarParentFingerprint.result?.let { parentResult ->
            ActionBarHookFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex
                    val targetRegister =
                        getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                    addInstruction(
                        targetIndex + 1,
                        "invoke-static {v$targetRegister}, $ACTIONBAR->hookActionBar(Landroid/view/ViewGroup;)V"
                    )
                }
            } ?: throw ActionBarHookFingerprint.exception
        } ?: throw ActionsBarParentFingerprint.exception

    }
}