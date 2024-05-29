package app.revanced.patches.music.utils.settings.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object GoogleApiActivityFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/GoogleApiActivity;")
                && methodDef.name == "onCreate"
    }
)
