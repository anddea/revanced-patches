package app.revanced.patches.youtube.player.ambientmode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object PowerSaveModeBroadcastReceiverFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
    strings = listOf("android.os.action.POWER_SAVE_MODE_CHANGED"),
    // There are two classes that inherit [BroadcastReceiver].
    // Check the method count to find the correct class.
    customFingerprint = { _, classDef ->
        classDef.superclass == "Landroid/content/BroadcastReceiver;"
                && classDef.methods.count() == 2
    }
)