package app.revanced.patches.music.misc.videoid.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.misc.videoid.fingerprint.MusicVideoIdFingerprint
import app.revanced.patches.shared.annotation.YouTubeMusicCompatibility
import app.revanced.util.integrations.Constants.MUSIC_UTILS_PATH
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("music-video-id-hook")
@Description("Hook to detect when the video id changes.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class MusicVideoIdPatch : BytecodePatch(
    listOf(
        MusicVideoIdFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        MusicVideoIdFingerprint.result?.let {
            insertIndex = it.scanResult.patternScanResult!!.endIndex

            with (it.mutableMethod) {
                insertMethod = this
                videoIdRegister = (implementation!!.instructions[insertIndex] as OneRegisterInstruction).registerA
            }
            offset++ // offset so setVideoId is called before any injected call
        } ?: return MusicVideoIdFingerprint.toErrorResult()
        
        injectCall("$INTEGRATIONS_CLASS_DESCRIPTOR->setVideoId(Ljava/lang/String;)V")

        return PatchResultSuccess()
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

