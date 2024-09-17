package app.revanced.patches.youtube.utils.fix.shortsplayback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.youtube.utils.fix.shortsplayback.fingerprints.ShortsPlaybackFingerprint
import app.revanced.util.injectLiteralInstructionBooleanCall

@Patch(
    description = "Fix issue with looping at the start of the video when applying default video quality to Shorts."
)
object ShortsPlaybackPatch : BytecodePatch(
    setOf(ShortsPlaybackFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        /**
         * This issue only affects some versions of YouTube.
         * Therefore, this patch only applies to versions that can resolve this fingerprint.
         *
         * RVX applies default video quality to Shorts as well, so this patch is required.
         */
        ShortsPlaybackFingerprint.result?.let {
            ShortsPlaybackFingerprint.injectLiteralInstructionBooleanCall(
                45387052,
                "0x0"
            )
        }

    }
}
