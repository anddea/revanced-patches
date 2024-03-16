package app.revanced.patches.youtube.overlaybutton.download.hook

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.overlaybutton.download.hook.fingerprints.OfflineVideoEndpointFingerprint
import app.revanced.patches.youtube.overlaybutton.download.hook.fingerprints.PlaylistOfflineDownloadOnClickFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.util.exception
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.Opcode

object DownloadButtonHookPatch : BytecodePatch(
    setOf(
        OfflineVideoEndpointFingerprint,
        PlaylistOfflineDownloadOnClickFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        val commonInstructions = """
            move-result v0
            if-eqz v0, :show_native_downloader
            return-void
            :show_native_downloader
            nop
        """

        // Get videoId and startVideoDownloadActivity
        OfflineVideoEndpointFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static/range {p3 .. p3}, $UTILS_PATH/HookDownloadButtonPatch;->startVideoDownloadActivity(Ljava/lang/String;)Z
                        $commonInstructions
                    """
                )
            }
        } ?: throw OfflineVideoEndpointFingerprint.exception

        // Get playlistId and startPlaylistDownloadActivity
        PlaylistOfflineDownloadOnClickFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                        instruction.opcode == Opcode.INVOKE_STATIC
                                && instruction.getReference<MethodReference>()?.name == "isEmpty"
                }

                val insertRegister = getInstruction<Instruction35c>(insertIndex).registerC
                addInstructionsWithLabels(
                    insertIndex,
                    """
                        invoke-static {v$insertRegister}, $UTILS_PATH/HookDownloadButtonPatch;->startPlaylistDownloadActivity(Ljava/lang/String;)Z
                        $commonInstructions
                    """
                )
            }
        } // Do not throw exception
    }
}
