package app.revanced.patches.youtube.layout.player.watermark.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.layout.player.watermark.bytecode.fingerprints.HideWatermarkFingerprint
import app.revanced.patches.youtube.layout.player.watermark.bytecode.fingerprints.HideWatermarkParentFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.PLAYER_LAYOUT

@Name("hide-channel-watermark-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideChannelWatermarkBytecodePatch : BytecodePatch(
    listOf(
        HideWatermarkParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        HideWatermarkFingerprint.resolve(context, HideWatermarkParentFingerprint.result!!.classDef)
        val result = HideWatermarkFingerprint.result
            ?: return PatchResultError("Required parent method could not be found.")

        val method = result.mutableMethod
        val line = method.implementation!!.instructions.size - 5

        method.removeInstruction(line)
        method.addInstructions(
            line, """
            invoke-static {}, $PLAYER_LAYOUT->hideChannelWatermark()Z
            move-result p2
        """
        )

        return PatchResultSuccess()
    }
}
