package app.revanced.patches.music.misc.splash.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MainActivityLaunchAnimation
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.indexOfFirstWideLiteralInstructionValue

/**
 * This fingerprint is compatible with YouTube Music v7.06.53+
 */
internal object CairoSplashAnimationConfigFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = handler@{ methodDef, _ ->
        if (methodDef.definingClass != "Lcom/google/android/apps/youtube/music/activities/MusicActivity;")
            return@handler false
        if (methodDef.name != "onCreate")
            return@handler false

        if (SettingsPatch.upward0720) {
            methodDef.indexOfFirstWideLiteralInstructionValue(MainActivityLaunchAnimation) >= 0
        } else {
            methodDef.indexOfFirstWideLiteralInstructionValue(45635386) >= 0
        }
    }
)