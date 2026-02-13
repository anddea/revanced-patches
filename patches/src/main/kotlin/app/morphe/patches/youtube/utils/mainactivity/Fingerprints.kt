package app.morphe.patches.youtube.utils.mainactivity

import app.morphe.util.fingerprint.legacyFingerprint

/**
 * 'WatchWhileActivity' has been renamed to 'MainActivity' in YouTube v18.48.xx+
 * This fingerprint was added to prepare for YouTube v18.48.xx+
 */
internal val mainActivityFingerprint = legacyFingerprint(
    name = "mainActivityFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    strings = listOf("PostCreateCalledKey"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("Activity;")
                && method.name == "onCreate"
    }
)
