package app.revanced.patches.music.misc.splash

import app.revanced.patches.music.utils.playservice.is_7_20_or_greater
import app.revanced.patches.music.utils.resourceid.mainActivityLaunchAnimation
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.indexOfFirstLiteralInstruction

/**
 * This fingerprint is compatible with YouTube Music v7.06.53+
 */
internal val cairoSplashAnimationConfigFingerprint = legacyFingerprint(
    name = "cairoSplashAnimationConfigFingerprint",
    returnType = "V",
    customFingerprint = handler@{ method, _ ->
        if (method.definingClass != "Lcom/google/android/apps/youtube/music/activities/MusicActivity;")
            return@handler false
        if (method.name != "onCreate")
            return@handler false

        if (is_7_20_or_greater) {
            method.indexOfFirstLiteralInstruction(mainActivityLaunchAnimation) >= 0
        } else {
            method.indexOfFirstLiteralInstruction(45635386) >= 0
        }
    }
)
