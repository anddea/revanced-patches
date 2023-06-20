package app.revanced.patches.youtube.utils.playeroverlay.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.playeroverlay.fingerprint.PlayerOverlaysOnFinishInflateFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH

@Name("player-overlays-hook")
@Description("Hook for adding custom overlays to the video player.")
@YouTubeCompatibility
@Version("0.0.1")
class PlayerOverlaysHookPatch : BytecodePatch(
    listOf(PlayerOverlaysOnFinishInflateFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        // hook YouTubePlayerOverlaysLayout.onFinishInflate()
        PlayerOverlaysOnFinishInflateFingerprint.result?.mutableMethod?.let {
            it.addInstruction(
                it.implementation!!.instructions.size - 2,
                "invoke-static { p0 }, $UTILS_PATH/PlayerOverlaysHookPatch;->YouTubePlayerOverlaysLayout_onFinishInflateHook(Ljava/lang/Object;)V"
            )
        } ?: return PlayerOverlaysOnFinishInflateFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}