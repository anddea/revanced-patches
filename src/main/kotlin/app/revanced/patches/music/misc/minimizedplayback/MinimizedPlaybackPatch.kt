package app.revanced.patches.music.misc.minimizedplayback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.misc.minimizedplayback.fingerprints.MinimizedPlaybackManagerFingerprint
import app.revanced.util.exception

@Patch(
    name = "Enable minimized playback",
    description = "Enables playback in miniplayer for Kids music.",
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.21.52",
                "6.22.52",
                "6.23.56",
                "6.25.53",
                "6.26.51",
                "6.27.54",
                "6.28.53",
                "6.29.58",
                "6.31.55",
                "6.33.52"
            ]
        )
    ]
)
@Suppress("unused")
object MinimizedPlaybackPatch : BytecodePatch(
    setOf(MinimizedPlaybackManagerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        MinimizedPlaybackManagerFingerprint.result?.mutableMethod?.addInstruction(
            0, "return-void"
        ) ?: throw MinimizedPlaybackManagerFingerprint.exception

    }
}
