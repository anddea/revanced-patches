package app.revanced.patches.music.utils.fix.timedlyrics

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.playservice.is_8_28_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall

val timedLyricsPatch = bytecodePatch(
    description = "timedLyricsPatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (!is_8_28_or_greater) {
            return@execute
        }
        /**
         * When these experimental flags are enabled, the real-time lyrics UI will break.
         */
        timedLyricsFingerprint.injectLiteralInstructionBooleanCall(
            TIMED_LYRICS_FEATURE_FLAG,
            "0x0"
        )
    }
}
