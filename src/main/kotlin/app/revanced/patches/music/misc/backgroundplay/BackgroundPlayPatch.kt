package app.revanced.patches.music.misc.backgroundplay

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.layout.minimizedplayback.fingerprints.KidsMinimizedPlaybackPolicyControllerFingerprint
import app.revanced.patches.music.misc.backgroundplay.fingerprints.BackgroundPlaybackParentFingerprint
import app.revanced.util.exception

@Patch(
    name = "Background play",
    description = "Enables playing music in the background.",
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object BackgroundPlayPatch : BytecodePatch(
    setOf(
        KidsMinimizedPlaybackPolicyControllerFingerprint,
        BackgroundPlaybackParentFingerprint,
    ),
) {
    override fun execute(context: BytecodeContext) {
        KidsMinimizedPlaybackPolicyControllerFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "return-void",
        ) ?: throw KidsMinimizedPlaybackPolicyControllerFingerprint.exception

        BackgroundPlaybackParentFingerprint.result?.mutableMethod?.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        ) ?: throw BackgroundPlaybackParentFingerprint.exception
    }
}