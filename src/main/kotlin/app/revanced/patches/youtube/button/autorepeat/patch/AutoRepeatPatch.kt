package app.revanced.patches.youtube.button.autorepeat.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.button.autorepeat.fingerprints.*
import app.revanced.util.integrations.Constants.UTILS_PATH
import app.revanced.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction31i

@Name("always-autorepeat")
@YouTubeCompatibility
@Version("0.0.1")
class AutoRepeatPatch : BytecodePatch(
    listOf(
        AutoNavInformerFingerprint,
        RepeatListenerFingerprint,
        VideoEndParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        VideoEndParentFingerprint.result?.classDef?.let { classDef ->
            VideoEndFingerprint.also {
                it.resolve(context, classDef)
            }.result?.mutableMethod?.let {
                it.addInstructionsWithLabels(
                    0, """
                    invoke-static {}, $VIDEO_PATH/VideoInformation;->shouldAutoRepeat()Z
                    move-result v0
                    if-eqz v0, :notrepeat
                    return-void
                """, ExternalLabel("notrepeat", it.getInstruction(0))
                )
            } ?: return VideoEndFingerprint.toErrorResult()
        } ?: return VideoEndParentFingerprint.toErrorResult()

        RepeatListenerFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex - 1
                val endIndex = it.scanResult.patternScanResult!!.endIndex

                val registerC = getInstruction<BuilderInstruction35c>(targetIndex).registerC
                val registerD = getInstruction<BuilderInstruction35c>(targetIndex).registerD

                val dummyRegister = (getInstruction(endIndex) as Instruction31i).registerA

                val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference

                addInstructionsWithLabels(
                    targetIndex + 1, """
                            invoke-static {}, $UTILS_PATH/EnableAutoRepeatPatch;->shouldAutoRepeat()Z
                            move-result v$dummyRegister
                            if-nez v$dummyRegister, :bypass
                            invoke-virtual {v$registerC, v$registerD}, $targetReference
                            """, ExternalLabel("bypass", getInstruction(targetIndex + 1))
                )
                removeInstruction(targetIndex)
            }
        } ?: return RepeatListenerFingerprint.toErrorResult()

        AutoNavInformerFingerprint.result?.mutableMethod?.let {
            val index = it.implementation!!.instructions.size - 1 - 1
            val register = it.getInstruction<OneRegisterInstruction>(index).registerA

            it.addInstructions(
                index + 1, """
                    invoke-static {v$register}, $UTILS_PATH/EnableAutoRepeatPatch;->enableAutoRepeat(Z)Z
                    move-result v0
                    """
            )
        } ?: return AutoNavInformerFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
