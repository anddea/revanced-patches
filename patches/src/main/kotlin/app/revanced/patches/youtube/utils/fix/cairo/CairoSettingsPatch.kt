package app.revanced.patches.youtube.utils.fix.cairo

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.backgroundplayback.backgroundPlaybackPatch
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.resolvable

val cairoSettingsPatch = bytecodePatch(
    description = "cairoSettingsPatch"
) {
    execute {
        /**
         * Cairo Fragment was added since YouTube v19.04.38.
         * Disable this for the following reasons:
         * 1. [backgroundPlaybackPatch] does not activate the Minimized playback setting of Cairo Fragment.
         * 2. Some patches implemented in RVX do not yet support Cairo Fragments.
         *
         * See <a href="https://github.com/inotia00/ReVanced_Extended/issues/2099">ReVanced_Extended#2099</a>
         * or <a href="https://github.com/qnblackcat/uYouPlus/issues/1468">uYouPlus#1468</a>
         * for screenshots of the Cairo Fragment.
         */
        if (carioFragmentConfigFingerprint.resolvable()) {
            carioFragmentConfigFingerprint.injectLiteralInstructionBooleanCall(
                45532100L,
                "0x0"
            )
        }
    }
}
