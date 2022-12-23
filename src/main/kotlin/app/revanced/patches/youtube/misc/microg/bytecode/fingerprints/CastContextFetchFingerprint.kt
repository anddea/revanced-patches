package app.revanced.patches.youtube.misc.microg.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object CastContextFetchFingerprint : MethodFingerprint(
    strings = listOf("Error fetching CastContext.")
)