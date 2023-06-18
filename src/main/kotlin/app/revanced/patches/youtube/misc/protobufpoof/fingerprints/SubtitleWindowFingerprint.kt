package app.revanced.patches.youtube.misc.protobufpoof.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object SubtitleWindowFingerprint : MethodFingerprint(
    parameters = listOf("I", "I", "I", "Z", "Z"),
    customFingerprint = { it, _ -> it.definingClass.endsWith("SubtitleWindowSettings;") && it.name == "<init>" }
)