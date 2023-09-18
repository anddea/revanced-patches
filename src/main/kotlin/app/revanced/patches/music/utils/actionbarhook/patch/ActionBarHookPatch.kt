package app.revanced.patches.music.utils.actionbarhook.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.utils.actionbarhook.fingerprints.ActionBarHookFingerprint
import app.revanced.patches.music.utils.fingerprints.ActionsBarParentFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.MUSIC_ACTIONBAR
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@DependsOn([SharedResourceIdPatch::class])
class ActionBarHookPatch : BytecodePatch(
    listOf(ActionsBarParentFingerprint)
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
                        "invoke-static {v$targetRegister}, $MUSIC_ACTIONBAR->hookActionBar(Landroid/view/ViewGroup;)V"
                    )
                }
            } ?: throw ActionBarHookFingerprint.exception
        } ?: throw ActionsBarParentFingerprint.exception

    }
}