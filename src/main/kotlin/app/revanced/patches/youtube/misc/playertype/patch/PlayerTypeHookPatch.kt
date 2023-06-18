package app.revanced.patches.youtube.misc.playertype.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.playertype.fingerprint.UpdatePlayerTypeFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH

@Name("player-type-hook")
@Description("Hook to get the current player type of WatchWhileActivity")
@YouTubeCompatibility
@Version("0.0.1")
class PlayerTypeHookPatch : BytecodePatch(
    listOf(
        UpdatePlayerTypeFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        // hook YouTubePlayerOverlaysLayout.updatePlayerLayout()
        UpdatePlayerTypeFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "invoke-static { p1 }, $UTILS_PATH/PlayerTypeHookPatch;->YouTubePlayerOverlaysLayout_updatePlayerTypeHookEX(Ljava/lang/Object;)V"
        ) ?: return UpdatePlayerTypeFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
