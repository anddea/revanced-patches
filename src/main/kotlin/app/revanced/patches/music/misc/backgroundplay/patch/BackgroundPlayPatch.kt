package app.revanced.patches.music.misc.backgroundplay.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.misc.backgroundplay.fingerprints.BackgroundPlaybackParentFingerprint
import app.revanced.patches.music.utils.annotations.MusicCompatibility

@Patch
@Name("Background play")
@Description("Enables playing music in the background.")
@MusicCompatibility
class BackgroundPlayPatch : BytecodePatch(
    listOf(BackgroundPlaybackParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        BackgroundPlaybackParentFingerprint.result?.let {
            with(
                context
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
        } ?: throw BackgroundPlaybackParentFingerprint.exception

    }
}