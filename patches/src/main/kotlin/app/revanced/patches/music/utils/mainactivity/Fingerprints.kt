package app.revanced.patches.music.utils.mainactivity

import app.revanced.util.fingerprint.legacyFingerprint

internal val mainActivityFingerprint = legacyFingerprint(
    name = "mainActivityFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    strings = listOf(
        "android.intent.action.MAIN",
        "FEmusic_home"
    ),
    customFingerprint = { method, classDef ->
        method.name == "onCreate" && classDef.endsWith("Activity;")
    }
)
