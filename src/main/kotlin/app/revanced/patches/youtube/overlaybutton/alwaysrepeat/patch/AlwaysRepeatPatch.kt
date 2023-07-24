package app.revanced.patches.youtube.overlaybutton.alwaysrepeat.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.overlaybutton.alwaysrepeat.fingerprints.AutoNavInformerFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoEndFingerprint
import app.revanced.patches.youtube.utils.fingerprints.VideoEndParentFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH
import app.revanced.util.integrations.Constants.VIDEO_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class AlwaysRepeatPatch : BytecodePatch(
    listOf(
        AutoNavInformerFingerprint,
        VideoEndParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        VideoEndParentFingerprint.result?.classDef?.let { classDef ->
            VideoEndFingerprint.also { it.resolve(context, classDef) }.result?.let {
                it.mutableMethod.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $VIDEO_PATH/VideoInformation;->shouldAlwaysRepeat()Z
                            move-result v0
                            if-eqz v0, :end
                            return-void
                            """, ExternalLabel("end", getInstruction(0))
                    )
                }
            } ?: return VideoEndFingerprint.toErrorResult()
        } ?: return VideoEndParentFingerprint.toErrorResult()

        AutoNavInformerFingerprint.result?.let {
            with(
                context
                    .toMethodWalker(it.method)
                    .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                    .getMethod() as MutableMethod
            ) {
                val index = implementation!!.instructions.size - 1 - 1
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $UTILS_PATH/AlwaysRepeatPatch;->enableAlwaysRepeat(Z)Z
                        move-result v0
                        """
                )
            }
        } ?: return AutoNavInformerFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
