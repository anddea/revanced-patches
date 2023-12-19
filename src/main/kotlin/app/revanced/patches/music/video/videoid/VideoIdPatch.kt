package app.revanced.patches.music.video.videoid

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.video.videoid.fingerprints.PlayerResponseModelImplGeneralFingerprint
import app.revanced.patches.music.video.videoid.fingerprints.VideoIdParentFingerprint
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.io.Closeable

object VideoIdPatch : BytecodePatch(
    setOf(
        PlayerResponseModelImplGeneralFingerprint,
        VideoIdParentFingerprint
    )
), Closeable {
    private var videoIdRegister = 0
    private var videoIdInsertIndex = 0
    private lateinit var videoIdMethod: MutableMethod

    private var backgroundPlaybackVideoIdRegister = 0
    private var backgroundPlaybackInsertIndex = 0
    private var backgroundPlaybackMethodName = ""
    private lateinit var backgroundPlaybackMethod: MutableMethod

    override fun execute(context: BytecodeContext) {

        VideoIdParentFingerprint.result?.let { result ->
            val targetIndex = result.scanResult.patternScanResult!!.endIndex
            val targetReference = result.mutableMethod.getInstruction<ReferenceInstruction>(targetIndex).reference
            val targetClass = (targetReference as FieldReference).type

            context.findClass(targetClass)!!
                .mutableClass.methods.first { method -> method.name == "handleVideoStageEvent" }
                .apply {
                    videoIdMethod = this
                    videoIdInsertIndex = implementation!!.instructions.indexOfLast { instruction ->
                        val invokeReference = ((instruction as? ReferenceInstruction)?.reference) as? MethodReference
                        backgroundPlaybackMethodName = invokeReference?.name.toString()

                        instruction.opcode == Opcode.INVOKE_INTERFACE
                                && invokeReference?.returnType == "Ljava/lang/String;"
                    } + 2

                    videoIdRegister = getInstruction<OneRegisterInstruction>(videoIdInsertIndex - 1).registerA
                }
        } ?: throw VideoIdParentFingerprint.exception

        PlayerResponseModelImplGeneralFingerprint.result
            ?.mutableClass?.methods?.find { method -> method.name == backgroundPlaybackMethodName }
            ?.apply {
                backgroundPlaybackMethod = this
                backgroundPlaybackInsertIndex = implementation!!.instructions.size - 1
                backgroundPlaybackVideoIdRegister = getInstruction<OneRegisterInstruction>(backgroundPlaybackInsertIndex).registerA
            } ?: throw PlayerResponseModelImplGeneralFingerprint.exception
    }

    override fun close() {
        backgroundPlaybackMethod.apply {
            val insertIndex = implementation!!.instructions.indexOfLast { instruction ->
                instruction.opcode == Opcode.IGET_OBJECT
            } + 1

            if (insertIndex != backgroundPlaybackInsertIndex) {
                addInstructionsWithLabels(
                    insertIndex, """
                        if-eqz v$backgroundPlaybackVideoIdRegister, :ignore
                        """, ExternalLabel("ignore", getInstruction(backgroundPlaybackInsertIndex))
                )
            }
        }
    }

    fun hookVideoId(
        methodDescriptor: String
    ) = videoIdMethod.addInstruction(
        videoIdInsertIndex++,
        "invoke-static {v$videoIdRegister}, $methodDescriptor"
    )

    fun hookBackgroundPlayVideoId(
        methodDescriptor: String
    ) = backgroundPlaybackMethod.addInstruction(
        backgroundPlaybackInsertIndex++, // move-result-object offset
        "invoke-static {v$backgroundPlaybackVideoIdRegister}, $methodDescriptor"
    )
}

