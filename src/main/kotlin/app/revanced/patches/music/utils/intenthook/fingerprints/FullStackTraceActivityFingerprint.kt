package app.revanced.patches.music.utils.intenthook.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object FullStackTraceActivityFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/FullStackTraceActivity;") && methodDef.name == "onCreate" }
)
