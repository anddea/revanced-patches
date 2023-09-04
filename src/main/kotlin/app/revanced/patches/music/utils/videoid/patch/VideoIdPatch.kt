package app.revanced.patches.music.utils.videoid.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.utils.videoid.fingerprint.VideoIdParentFingerprint
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

class VideoIdPatch : BytecodePatch(
    listOf(VideoIdParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        VideoIdParentFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex

                val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetClass = (targetReference as FieldReference).type

                insertMethod = context
                    .findClass(targetClass)!!
                    .mutableClass.methods.first { method ->
                        method.name == "handleVideoStageEvent"
                    }
            }
        } ?: throw VideoIdParentFingerprint.exception

        insertMethod.apply {
            for (index in implementation!!.instructions.size - 1 downTo 0) {
                if (getInstruction(index).opcode != Opcode.INVOKE_INTERFACE) continue

                val targetReference = getInstruction<ReferenceInstruction>(index).reference

                if (!targetReference.toString().endsWith("Ljava/lang/String;")) continue

                insertIndex = index + 1
                videoIdRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                break
            }
            offset++ // offset so setVideoId is called before any injected call
        }

        injectCall("$INTEGRATIONS_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")

    }

    companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$MUSIC_UTILS_PATH/VideoInformation;"

        private var offset = 0

        private var insertIndex: Int = 0
        private var videoIdRegister: Int = 0
        private lateinit var insertMethod: MutableMethod

        /**
         * Adds an invoke-static instruction, called with the new id when the video changes
         * @param methodDescriptor which method to call. Params have to be `Ljava/lang/String;`
         */
        fun injectCall(
            methodDescriptor: String
        ) {
            insertMethod.addInstructions(
                insertIndex + offset, // move-result-object offset
                "invoke-static {v$videoIdRegister}, $methodDescriptor"
            )
        }
    }
}

