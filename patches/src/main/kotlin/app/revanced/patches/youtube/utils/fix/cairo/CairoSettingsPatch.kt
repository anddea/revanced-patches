package app.revanced.patches.youtube.utils.fix.cairo

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.misc.backgroundplayback.backgroundPlaybackPatch
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall

/**
 * As of 2025, responses to [Account/Get Setting](https://youtubei.googleapis.com/youtubei/v1/account/get_setting)
 * requests no longer include the Preference 'Autoplay' (YouTube 19.34+).
 *
 * In YouTube 19.34+, the Preference 'Playback' of the Cairo fragment replaces the Preference 'Autoplay'.
 *
 * Since RVX disables the Cairo fragment,
 * users who have newly installed RVX 19.34+ will no longer be able to turn 'Autoplay next video' on or off in YouTube settings.
 *
 * As a workaround, [cairoSettingsPatch] has been replaced by [cairoFragmentPatch].
 */
@Deprecated("Use 'cairoFragmentPatch' instead.")
@Suppress("unused")
val cairoSettingsPatch = bytecodePatch(
    description = "cairoSettingsPatch"
) {
    dependsOn(cairoFragmentPatch)

    execute {
        if (true) {
            return@execute
        }

        /**
         * Cairo fragment was added since YouTube v19.04.38.
         * Disable this for the following reasons:
         * 1. [backgroundPlaybackPatch] does not activate the Minimized playback setting of Cairo Fragment.
         * 2. Some patches implemented in RVX do not yet support Cairo fragments.
         *
         * See <a href="https://github.com/inotia00/ReVanced_Extended/issues/2099">ReVanced_Extended#2099</a>
         * or <a href="https://github.com/qnblackcat/uYouPlus/issues/1468">uYouPlus#1468</a>
         * for screenshots of the Cairo fragment.
         */
        cairoFragmentConfigFingerprint.injectLiteralInstructionBooleanCall(
            CAIRO_FRAGMENT_FEATURE_FLAG,
            "0x0"
        )
    }
}
