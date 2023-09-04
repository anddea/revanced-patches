package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsInfoPanelFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerInfoPanel
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class ShortsInfoPanelPatch : BytecodePatch(
    listOf(ShortsInfoPanelFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ShortsInfoPanelFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getWideLiteralIndex(ReelPlayerInfoPanel) + 3
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerInfoPanel(Landroid/view/ViewGroup;)Landroid/view/ViewGroup;
                        move-result-object v$insertRegister
                        """
                )
            }
        } ?: throw ShortsInfoPanelFingerprint.exception

    }
}
