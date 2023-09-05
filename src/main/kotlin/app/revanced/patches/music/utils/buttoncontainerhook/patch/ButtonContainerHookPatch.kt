package app.revanced.patches.music.utils.buttoncontainerhook.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.music.utils.buttoncontainerhook.fingerprints.ButtonContainerHookFingerprint
import app.revanced.patches.music.utils.fingerprints.ActionsContainerParentFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.MUSIC_BUTTON_CONTAINER
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@DependsOn([SharedResourceIdPatch::class])
class ButtonContainerHookPatch : BytecodePatch(
    listOf(ActionsContainerParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ActionsContainerParentFingerprint.result?.let { parentResult ->
            ButtonContainerHookFingerprint.also {
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
                        "invoke-static {v$targetRegister}, $MUSIC_BUTTON_CONTAINER->hookButtonContainer(Landroid/view/ViewGroup;)V"
                    )
                }
            } ?: throw ButtonContainerHookFingerprint.exception
        } ?: throw ActionsContainerParentFingerprint.exception

    }
}