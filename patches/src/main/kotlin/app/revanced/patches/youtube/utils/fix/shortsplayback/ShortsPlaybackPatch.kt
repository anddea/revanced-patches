package app.revanced.patches.youtube.utils.fix.shortsplayback

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.resolvable

val shortsPlaybackPatch = bytecodePatch(
    description = "shortsPlaybackPatch"
) {

    execute {
        /**
         * This issue only affects some versions of YouTube.
         * Therefore, this patch only applies to versions that can resolve this fingerprint.
         *
         * RVX applies default video quality to Shorts as well, so this patch is required.
         */
        if (shortsPlaybackFingerprint.resolvable()) {
            shortsPlaybackFingerprint.injectLiteralInstructionBooleanCall(
                45387052L,
                "0x0"
            )
        }
    }
}
