package app.revanced.patches.music.video.videoid

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.video.videoid.fingerprints.VideoIdFingerprint
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

object VideoIdPatch : BytecodePatch(
    setOf(VideoIdFingerprint)
) {
    private var videoIdRegister = 0
    private var videoIdInsertIndex = 0
    private lateinit var videoIdMethod: MutableMethod

    override fun execute(context: BytecodeContext) {

        VideoIdFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                videoIdMethod = this
                videoIdInsertIndex = it.scanResult.patternScanResult!!.startIndex + 2
                videoIdRegister = getInstruction<OneRegisterInstruction>(videoIdInsertIndex - 1).registerA
            }
        }
    }

    fun hookVideoId(
        methodDescriptor: String
    ) = videoIdMethod.addInstruction(
        videoIdInsertIndex++,
        "invoke-static {v$videoIdRegister}, $methodDescriptor"
    )
}

