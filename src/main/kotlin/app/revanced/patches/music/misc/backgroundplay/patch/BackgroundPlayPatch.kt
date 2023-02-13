package app.revanced.patches.music.misc.backgroundplay.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.misc.backgroundplay.fingerprints.BackgroundPlaybackParentFingerprint
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import app.revanced.shared.extensions.toErrorResult

@Patch
@Name("background-play")
@Description("Enables playing music in the background.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class BackgroundPlayPatch : BytecodePatch(
    listOf(
        BackgroundPlaybackParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        BackgroundPlaybackParentFingerprint.result?.let {
            with(context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex, true)
                .getMethod() as MutableMethod
            ) {
                addInstructions(
                    0, """
                        const/4 v0, 0x1
                        return v0
                        """
                )
            }
        } ?: return BackgroundPlaybackParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}