package app.revanced.patches.shared.captions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.shared.captions.fingerprints.StartVideoInformerFingerprint
import app.revanced.patches.shared.captions.fingerprints.SubtitleTrackFingerprint
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

abstract class BaseAutoCaptionsPatch(
    private val classDescriptor: String
) : BytecodePatch(
    setOf(
        StartVideoInformerFingerprint,
        SubtitleTrackFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        StartVideoInformerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {}, $classDescriptor->prefetchSubtitleTrack()V"
                )
            }
        }

        SubtitleTrackFingerprint.resultOrThrow().let {
            val targetMethod = it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex + 1)

            targetMethod.apply {
                val targetIndex = indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_VIRTUAL
                            && ((this as? ReferenceInstruction)?.reference as? MethodReference)?.returnType == "Z"
                } + 1
                val insertRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$insertRegister}, $classDescriptor->disableAutoCaptions(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }

    }
}