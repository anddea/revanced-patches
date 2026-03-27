package app.morphe.patches.music.general.splash

import app.morphe.patches.music.utils.resourceid.mainActivityLaunchAnimation
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.indexOfFirstLiteralInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val CAIRO_SPLASH_ANIMATION_FEATURE_FLAG = 45635386L

/**
 * This fingerprint is compatible with YouTube Music v7.06.53+
 */
internal val cairoSplashAnimationConfigFingerprint = legacyFingerprint(
    name = "cairoSplashAnimationConfigFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    strings = listOf("sa_e"),
    customFingerprint = handler@{ method, _ ->
        if (method.definingClass != "Lcom/google/android/apps/youtube/music/activities/MusicActivity;")
            return@handler false
        if (method.name != "onCreate")
            return@handler false
        if (indexOfSetContentViewInstruction(method) < 0)
            return@handler false

        method.indexOfFirstLiteralInstruction(CAIRO_SPLASH_ANIMATION_FEATURE_FLAG) >= 0
                || method.indexOfFirstLiteralInstruction(mainActivityLaunchAnimation) >= 0
    }
)

internal fun indexOfSetContentViewInstruction(method: Method, startIndex: Int? = null) =
    method.indexOfFirstInstructionReversed(startIndex) {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setContentView"
    }

