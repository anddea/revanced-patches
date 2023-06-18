package app.revanced.patches.music.misc.sharebuttonhook.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object FullStackTraceActivityFingerprint : MethodFingerprint(
    customFingerprint = { it, _ ->
        it.definingClass.endsWith("FullStackTraceActivity;") && it.name == "onCreate"
    }
)
