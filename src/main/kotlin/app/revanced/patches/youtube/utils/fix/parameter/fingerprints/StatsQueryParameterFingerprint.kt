package app.revanced.patches.youtube.utils.fix.parameter.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object StatsQueryParameterFingerprint : MethodFingerprint(
    strings = listOf("adunit"),
)