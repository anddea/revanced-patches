package app.revanced.patches.youtube.utils.playertype.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.playertype.fingerprint.PlayerTypeFingerprint
import app.revanced.patches.youtube.utils.playertype.fingerprint.VideoStateFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.util.integrations.Constants.UTILS_PATH
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@DependsOn([SharedResourceIdPatch::class])
class PlayerTypeHookPatch : BytecodePatch(
    listOf(
        PlayerTypeFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PlayerTypeFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstruction(
                    0,
                    "invoke-static {p1}, $INTEGRATIONS_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V"
                )
            }
        } ?: return PlayerTypeFingerprint.toErrorResult()

        YouTubeControlsOverlayFingerprint.result?.let { parentResult ->
            VideoStateFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val endIndex = it.scanResult.patternScanResult!!.endIndex
                    val videoStateFieldName =
                        getInstruction<ReferenceInstruction>(endIndex).reference
                    addInstructions(
                        0, """
                        iget-object v0, p1, $videoStateFieldName  # copy VideoState parameter field
                        invoke-static {v0}, $INTEGRATIONS_CLASS_DESCRIPTOR->setVideoState(Ljava/lang/Enum;)V
                        """
                    )
                }
            } ?: return VideoStateFingerprint.toErrorResult()
        } ?: return YouTubeControlsOverlayFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    companion object {
        private const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$UTILS_PATH/PlayerTypeHookPatch;"
    }
}
