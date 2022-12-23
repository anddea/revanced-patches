package app.revanced.patches.youtube.misc.customvideobuffer.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.customvideobuffer.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.MISC_PATH
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Name("custom-video-buffer-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class CustomVideoBufferBytecodePatch : BytecodePatch(
    listOf(
        MaxBufferFingerprint,
        PlaybackBufferFingerprint,
        ReBufferFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        execMaxBuffer()
        execPlaybackBuffer()
        execReBuffer()
        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_BUFFER_CLASS_DESCRIPTOR =
            "$MISC_PATH/CustomVideoBufferPatch;"
    }
    private fun execMaxBuffer() {
        val (method, result) = MaxBufferFingerprint.unwrap(true, -1)
        val (index, register) = result

        method.addInstructions(
            index + 1, """
           invoke-static {}, $INTEGRATIONS_BUFFER_CLASS_DESCRIPTOR->setMaxBuffer()I
           move-result v$register
        """
        )
    }

    private fun execPlaybackBuffer() {
        val (method, result) = PlaybackBufferFingerprint.unwrap()
        val (index, register) = result

        method.addInstructions(
            index + 1, """
           invoke-static {}, $INTEGRATIONS_BUFFER_CLASS_DESCRIPTOR->setPlaybackBuffer()I
           move-result v$register
        """
        )
    }

    private fun execReBuffer() {
        val (method, result) = ReBufferFingerprint.unwrap()
        val (index, register) = result

        method.addInstructions(
            index + 1, """
           invoke-static {}, $INTEGRATIONS_BUFFER_CLASS_DESCRIPTOR->setReBuffer()I
           move-result v$register
        """
        )
    }

    private fun MethodFingerprint.unwrap(
        forEndIndex: Boolean = false,
        offset: Int = 0
    ): Pair<MutableMethod, Pair<Int, Int>> {
        val result = this.result!!
        val method = result.mutableMethod
        val scanResult = result.scanResult.patternScanResult!!
        val index = (if (forEndIndex) scanResult.endIndex else scanResult.startIndex) + offset

        val register = (method.instruction(index) as OneRegisterInstruction).registerA

        return method to (index to register)
    }
}
