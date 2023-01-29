package app.revanced.patches.youtube.misc.customvideobuffer.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprintResult
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.customvideobuffer.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
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

        MaxBufferFingerprint.result?.injectMaxBuffer(context) ?: return MaxBufferFingerprint.toErrorResult()

        arrayOf(
            PlaybackBufferFingerprint to "setPlaybackBuffer",
            ReBufferFingerprint to "setReBuffer"
        ).map { (fingerprint, name) ->
            fingerprint.result?.mutableMethod?.insertOverride(name) ?: return fingerprint.toErrorResult()
        }

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_BUFFER_CLASS_DESCRIPTOR =
            "$MISC_PATH/CustomVideoBufferPatch;"
    }

    private fun MethodFingerprintResult.injectMaxBuffer(
        context: BytecodeContext
    ) {
        val insertMethod = context.toMethodWalker(this.method)
            .nextMethod(this.scanResult.patternScanResult!!.endIndex, true)
            .getMethod() as MutableMethod

        insertMethod.insertOverride("setMaxBuffer")
    }

    private fun MutableMethod.insertOverride(
        descriptor: String
    ) {
        val index = this.implementation!!.instructions.size - 1 - 2
        val register = (this.instruction(index) as OneRegisterInstruction).registerA

        this.addInstructions(
            index,
            """
                invoke-static {}, $INTEGRATIONS_BUFFER_CLASS_DESCRIPTOR->$descriptor()I
                move-result v$register
                """
        )
    }
}
