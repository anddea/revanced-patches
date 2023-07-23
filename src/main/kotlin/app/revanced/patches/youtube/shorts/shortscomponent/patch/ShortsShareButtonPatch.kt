package app.revanced.patches.youtube.shorts.shortscomponent.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.shorts.shortscomponent.fingerprints.ShortsShareFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelDynShare
import app.revanced.util.bytecode.getWideLiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class ShortsShareButtonPatch : BytecodePatch(
    listOf(ShortsShareFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ShortsShareFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = getWideLiteralIndex(ReelDynShare) - 2
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $SHORTS->hideShortsPlayerShareButton(Landroid/view/View;)V"
                )

            }
        } ?: return ShortsShareFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
