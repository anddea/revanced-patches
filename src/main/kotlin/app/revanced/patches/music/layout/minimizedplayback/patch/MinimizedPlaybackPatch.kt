package app.revanced.patches.music.layout.minimizedplayback.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.layout.minimizedplayback.fingerprints.MinimizedPlaybackManagerFingerprint
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("minimized-playback-music")
@Description("Enables minimized playback on Kids music.")
@YouTubeMusicCompatibility
@Version("0.0.1")
class MinimizedPlaybackPatch : BytecodePatch(
    listOf(
        MinimizedPlaybackManagerFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        MinimizedPlaybackManagerFingerprint.result!!.mutableMethod.addInstruction(
            0, "return-void"
        )

        return PatchResultSuccess()
    }
}
