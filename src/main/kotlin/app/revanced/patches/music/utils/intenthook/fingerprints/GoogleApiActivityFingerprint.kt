package app.revanced.patches.music.utils.intenthook.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object GoogleApiActivityFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/GoogleApiActivity;") && methodDef.name == "onCreate"
    }
)
