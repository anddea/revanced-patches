package app.revanced.patches.youtube.utils.fix.protobufpoof.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object SubtitleWindowFingerprint : MethodFingerprint(
    parameters = listOf("I", "I", "I", "Z", "Z"),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("SubtitleWindowSettings;") && methodDef.name == "<init>" }
)