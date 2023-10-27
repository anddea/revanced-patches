package app.revanced.patches.music.utils.videotype

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.music.utils.videotype.fingerprint.VideoTypeFingerprint
import app.revanced.patches.music.utils.videotype.fingerprint.VideoTypeParentFingerprint
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object VideoTypeHookPatch : BytecodePatch(
    setOf(VideoTypeParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        VideoTypeParentFingerprint.result?.let { parentResult ->
            VideoTypeFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val videoTypeIndex = it.scanResult.patternScanResult!!.endIndex
                    val videoTypeRegister =
                        getInstruction<OneRegisterInstruction>(videoTypeIndex).registerA

                    addInstructions(
                        videoTypeIndex + 1, """
                            invoke-static {v$videoTypeRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->setVideoType(Ljava/lang/Enum;)V
                            return-object v$videoTypeRegister
                            """
                    )
                    removeInstruction(videoTypeIndex)
                }
            } ?: throw VideoTypeFingerprint.exception
        } ?: throw VideoTypeParentFingerprint.exception
    }

    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MUSIC_UTILS_PATH/VideoTypeHookPatch;"
}
