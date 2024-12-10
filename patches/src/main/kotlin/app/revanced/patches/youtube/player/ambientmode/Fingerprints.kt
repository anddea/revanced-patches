package app.revanced.patches.youtube.player.ambientmode

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val ambientModeInFullscreenFingerprint = legacyFingerprint(
    name = "ambientModeInFullscreenFingerprint",
    returnType = "V",
    literals = listOf(45389368L),
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