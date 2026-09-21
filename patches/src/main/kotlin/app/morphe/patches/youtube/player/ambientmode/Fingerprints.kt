package app.morphe.patches.youtube.player.ambientmode

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import com.android.tools.smali.dexlib2.AccessFlags

internal const val AMBIENT_MODE_IN_FULLSCREEN_FEATURE_FLAG = 45389368L

internal object AmbientModeFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45376186L),
    )
)

internal val ambientModeInFullscreenFingerprint = Fingerprint(
    returnType = "V",
    filters = listOf(literal(AMBIENT_MODE_IN_FULLSCREEN_FEATURE_FLAG)),
)

internal val powerSaveModeBroadcastReceiverFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    strings = listOf("android.os.action.POWER_SAVE_MODE_CHANGED"),
    // There are two classes that inherit [BroadcastReceiver].
    // Check the method count to find the correct class.
    custom = { _, classDef ->
        classDef.superclass == "Landroid/content/BroadcastReceiver;" &&
                classDef.methods.count() == 2
    }
)

internal val powerSaveModeSyntheticFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("android.os.action.POWER_SAVE_MODE_CHANGED")
)

internal val setFullScreenBackgroundColorFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = listOf("Z", "I", "I", "I", "I"),
    custom = { method, classDef ->
        classDef.type.endsWith("/YouTubePlayerViewNotForReflection;")
                && method.name == "onLayout"
    },
)
