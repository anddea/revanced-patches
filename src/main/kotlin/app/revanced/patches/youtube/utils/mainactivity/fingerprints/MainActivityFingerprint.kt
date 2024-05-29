package app.revanced.patches.youtube.utils.mainactivity.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

/**
 * 'WatchWhileActivity' has been renamed to 'MainActivity' in YouTube v18.48.xx+
 * This fingerprint was added to prepare for YouTube v18.48.xx+
 */
internal object MainActivityFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    strings = listOf("PostCreateCalledKey"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("Activity;")
                && methodDef.name == "onCreate"
    }
)