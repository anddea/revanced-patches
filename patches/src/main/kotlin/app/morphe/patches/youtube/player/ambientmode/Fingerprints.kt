package app.morphe.patches.youtube.player.ambientmode

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal const val AMBIENT_MODE_IN_FULLSCREEN_FEATURE_FLAG = 45389368L

internal val ambientModeInFullscreenFingerprint = legacyFingerprint(
    name = "ambientModeInFullscreenFingerprint",
    returnType = "V",
    literals = listOf(AMBIENT_MODE_IN_FULLSCREEN_FEATURE_FLAG),
)

internal val powerSaveModeBroadcastReceiverFingerprint = legacyFingerprint(
    name = "powerSaveModeBroadcastReceiverFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    strings = listOf("android.os.action.POWER_SAVE_MODE_CHANGED"),
    // There are two classes that inherit [BroadcastReceiver].
    // Check the method count to find the correct class.
    customFingerprint = { _, classDef ->
        classDef.superclass == "Landroid/content/BroadcastReceiver;" &&
                classDef.methods.count() == 2
    }
)

internal val powerSaveModeSyntheticFingerprint = legacyFingerprint(
    name = "powerSaveModeSyntheticFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("android.os.action.POWER_SAVE_MODE_CHANGED")
)

internal val setFullScreenBackgroundColorFingerprint = legacyFingerprint(
    name = "setFullScreenBackgroundColorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("Z", "I", "I", "I", "I"),
    customFingerprint = { method, classDef ->
        classDef.type.endsWith("/YouTubePlayerViewNotForReflection;")
                && method.name == "onLayout"
    },
)