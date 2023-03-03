package app.revanced.patches.youtube.button.autorepeat.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.*
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.VideoEndFingerprint
import app.revanced.patches.shared.fingerprints.VideoEndParentFingerprint
import app.revanced.patches.youtube.button.autorepeat.fingerprints.*
import app.revanced.util.integrations.Constants.UTILS_PATH
import app.revanced.util.integrations.Constants.VIDEO_PATH
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
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
                it.addInstructions(
                    0, """
                    invoke-static {}, $VIDEO_PATH/VideoInformation;->shouldAutoRepeat()Z
                    move-result v0
                    if-eqz v0, :notrepeat
                    return-void
                """, listOf(ExternalLabel("notrepeat", it.instruction(0)))
                )
            } ?: return VideoEndFingerprint.toErrorResult()
        } ?: return VideoEndParentFingerprint.toErrorResult()

        RepeatListenerFingerprint.result?.let {
            val targetIndex = it.scanResult.patternScanResult!!.startIndex - 1
            val endIndex = it.scanResult.patternScanResult!!.endIndex
                with (it.mutableMethod) {
                    val targetReference = (instruction(targetIndex) as BuilderInstruction35c).reference.toString()

                    val firstRegister = (instruction(targetIndex) as BuilderInstruction35c).registerC
                    val secondRegister = (instruction(targetIndex) as BuilderInstruction35c).registerD

                    val dummyRegister = (instruction(endIndex) as Instruction31i).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {}, $UTILS_PATH/EnableAutoRepeatPatch;->shouldAutoRepeat()Z
                            move-result v$dummyRegister
                            if-nez v$dummyRegister, :bypass
                            invoke-virtual {v$firstRegister, v$secondRegister}, $targetReference
                            """, listOf(ExternalLabel("bypass", instruction(targetIndex + 1)))
                    )
                    removeInstruction(targetIndex)
                }
        } ?: return RepeatListenerFingerprint.toErrorResult()

        AutoNavInformerFingerprint.result?.mutableMethod?.let {
            with (it.implementation!!.instructions) {
                val index = this.size - 1 - 1
                val register = (this[index] as OneRegisterInstruction).registerA
                it.addInstructions(
                    index + 1, """
                    invoke-static {v$register}, $UTILS_PATH/EnableAutoRepeatPatch;->enableAutoRepeat(Z)Z
                    move-result v0
                """
                )

            }
        } ?: return AutoNavInformerFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
